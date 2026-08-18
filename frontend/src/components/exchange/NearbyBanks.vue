<script setup>
import { ref, onMounted, watch, computed, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { useExchangeStore } from '@/stores/exchange';
import { fetchNearbyBanks, fetchExchangeEstimate } from '@/api/exchange';

const router = useRouter();
const exchange = useExchangeStore();
const query = ref('');
const fetchedBanks = ref([]);
const currentRadius = ref(2000);
const userLocation = ref(null);

const DEFAULT_LAT = 37.497942; // 기본 좌표 (강남역)
const DEFAULT_LNG = 127.027621;

// ⚡ UI 로딩 상태 및 Fallback 알림 상태
const isGeoLoading = ref(false);
const isLocationFallback = ref(false);

// ⚡ GPS 위치 수신 시간(5초)을 충분히 부여하고 시각적 로딩 처리
const getUserPosition = () => {
  isGeoLoading.value = true;
  return new Promise((resolve) => {
    if (!navigator.geolocation) {
      isLocationFallback.value = true;
      isGeoLoading.value = false;
      resolve({ lat: DEFAULT_LAT, lng: DEFAULT_LNG });
      return;
    }

    navigator.geolocation.getCurrentPosition(
      (position) => {
        isGeoLoading.value = false;
        isLocationFallback.value = false;
        resolve({
          lat: position.coords.latitude,
          lng: position.coords.longitude,
        });
      },
      (error) => {
        isGeoLoading.value = false;
        isLocationFallback.value = true; // 실패 시 안내 토스트 표출

        if (error.code === 3) {
          console.log('📍 GPS 수신 시간 초과로 기본 위치를 적용합니다.');
        } else {
          console.warn('Geolocation failure:', error.message);
        }
        resolve({ lat: DEFAULT_LAT, lng: DEFAULT_LNG }); // 기본 위치 반환
      },
      // 💡 PC 테스트 환경 및 수신 안정성을 고려하여 timeout을 5000ms(5초)로 부여
      { enableHighAccuracy: false, timeout: 5000, maximumAge: 60000 },
    );
  });
};

const getDistance = (lat1, lng1, lat2, lng2) => {
  const R = 6371e3;
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLng = ((lng2 - lng1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos((lat1 * Math.PI) / 180) *
      Math.cos((lat2 * Math.PI) / 180) *
      Math.sin(dLng / 2) *
      Math.sin(dLng / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
};

const formatDistance = (meters) => {
  if (!meters && meters !== 0) return '0m';
  if (meters >= 1000) {
    return `${(meters / 1000).toFixed(1)}km`;
  }
  return `${Math.round(meters)}m`;
};

const getDynamicRadius = () => {
  if (!map) return 2000;
  const center = map.getCenter();
  const bounds = map.getBounds();
  const sw = bounds.getSouthWest();

  const radius = getDistance(
    center.getLat(),
    center.getLng(),
    sw.getLat(),
    sw.getLng(),
  );
  return Math.min(20000, Math.max(1000, radius));
};

const displayRadius = computed(() => formatDistance(currentRadius.value));

const sortedBanks = computed(() => {
  return [...fetchedBanks.value].sort((a, b) => a.distance - b.distance);
});

const mapContainer = ref(null);
let map = null;
let markers = [];
let currentPositionMarker = null;

const showSearchThisAreaBtn = ref(false);
const mapLevel = ref(4);

const isTooWide = computed(() => mapLevel.value > 6);

const handleZoomChanged = () => {
  if (map) {
    mapLevel.value = map.getLevel();
  }
};

const getSvgIconUri = (fillColor, centerColor, isLarge = false) => {
  const width = isLarge ? 28 : 22;
  const height = isLarge ? 36 : 28;
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 24 32">
    <path d="M12 0C5.37 0 0 5.37 0 12c0 9.3 12 20 12 20s12-10.7 12-20c0-6.63-5.37-12-12-12z" fill="${fillColor}" stroke="#FFFFFF" stroke-width="1.5"/>
    <circle cx="12" cy="12" r="4.5" fill="${centerColor}"/>
  </svg>`;
  return 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svg);
};

const unselectedMarkerImage = () => {
  return new window.kakao.maps.MarkerImage(
    getSvgIconUri('#1870E8', '#FFFFFF', false),
    new window.kakao.maps.Size(22, 28),
    { offset: new window.kakao.maps.Point(11, 28) },
  );
};

const selectedMarkerImage = () => {
  return new window.kakao.maps.MarkerImage(
    getSvgIconUri('#FFCC00', '#4B433F', true),
    new window.kakao.maps.Size(18, 24),
    { offset: new window.kakao.maps.Point(9, 24) },
  );
};

let searchAreaCircle = null; // 검색 반경 원형 시각화 객체

// 1. 기존의 fitMapToSearchArea 함수 내부를 간소화합니다.
// (지도를 강제로 확대/축소시키는 map.setBounds()를 제거)
const fitMapToSearchArea = (lat, lng, radius) => {
  if (!map) return;

  const centerPosition = new window.kakao.maps.LatLng(lat, lng);

  // 기존 원 제거 후 다시 그리기
  if (searchAreaCircle) {
    searchAreaCircle.setMap(null);
  }

  searchAreaCircle = new window.kakao.maps.Circle({
    center: centerPosition,
    radius: radius,
    strokeWeight: 1.5,
    strokeColor: '#1870e8',
    strokeOpacity: 0.3,
    fillColor: '#1870e8',
    fillOpacity: 0.04,
  });
  searchAreaCircle.setMap(map);

  // ❌ [원인 제거] map.setBounds(searchAreaCircle.getBounds());
  // 강제 확대/축소를 하지 않고 사용자가 지정했거나 설정된 zoom level을 그대로 유지합니다.
};

// 2. 내 위치로 버튼 클릭 시 적정 확대 레벨(4) 설정
const moveToCurrentLocation = () => {
  if (navigator.geolocation) {
    isGeoLoading.value = true;
    navigator.geolocation.getCurrentPosition(
      (position) => {
        isGeoLoading.value = false;
        isLocationFallback.value = false;
        const lat = position.coords.latitude;
        const lng = position.coords.longitude;
        userLocation.value = { lat, lng };

        if (map) {
          map.setCenter(new window.kakao.maps.LatLng(lat, lng));
          map.setLevel(4); // 💡 적정 확대 레벨(동네 수준)로 고정!
        }

        if (currentPositionMarker) {
          currentPositionMarker.setPosition(
            new window.kakao.maps.LatLng(lat, lng),
          );
        }

        // 기본 반경(2000m = 2km)으로 탐색
        loadBanks(lat, lng, 2000);
      },
      (error) => {
        isGeoLoading.value = false;
        isLocationFallback.value = true;
        console.warn('Geolocation error:', error);
      },
      { enableHighAccuracy: false, timeout: 5000, maximumAge: 0 },
    );
  }
};

const mapLoadError = ref(false);
const kakaoMapKey = import.meta.env.VITE_KAKAO_MAP_KEY?.trim();

const loadKakaoMap = () => {
  return new Promise((resolve, reject) => {
    if (window.kakao && window.kakao.maps) {
      resolve();
      return;
    }

    if (!kakaoMapKey) {
      reject(new Error('VITE_KAKAO_MAP_KEY 환경변수가 설정되지 않았습니다.'));
      return;
    }

    const timeout = setTimeout(() => {
      reject(new Error('지도 서비스 로드 시간 초과'));
    }, 7000);

    const cleanup = () => clearTimeout(timeout);

    const existingScript = document.querySelector(
      'script[src*="dapi.kakao.com"]',
    );

    if (existingScript) {
      const handleLoad = () => {
        cleanup();
        resolve();
      };
      const handleError = () => {
        cleanup();
        reject(new Error('기존 지도 스크립트 로드 실패'));
      };

      existingScript.addEventListener('load', handleLoad);
      existingScript.addEventListener('error', handleError);
      return;
    }

    const script = document.createElement('script');
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${encodeURIComponent(kakaoMapKey)}&autoload=false&libraries=services`;

    script.onload = () => {
      cleanup();
      resolve();
    };

    script.onerror = () => {
      cleanup();
      reject(new Error('지도 스크립트 네트워크 로드 실패'));
    };

    document.head.appendChild(script);
  });
};

let currentBanksRequestId = 0;

const loadBanks = async (
  lat,
  lng,
  searchRadius = null,
  isManualSearch = false,
) => {
  const requestId = ++currentBanksRequestId;
  try {
    const radius = searchRadius || getDynamicRadius();
    currentRadius.value = radius;
    const data = await fetchNearbyBanks(lat, lng, radius);

    if (requestId !== currentBanksRequestId) return;

    fetchedBanks.value = (data || []).map((bank) => {
      const finalDistance = userLocation.value
        ? getDistance(
            userLocation.value.lat,
            userLocation.value.lng,
            bank.latitude,
            bank.longitude,
          )
        : bank.distance || 0;

      return {
        ...bank,
        distance: finalDistance,
      };
    });

    updateMapMarkers();
    showSearchThisAreaBtn.value = false;

    // 🎯 [이 지역 재검색] 버튼 클릭(isManualSearch = true) 시에는
    // 사용자가 직접 맞춘 지도 스코프(화면)를 강제로 바꾸지(Reset) 않고 그대로 유지합니다!
    if (!isManualSearch) {
      fitMapToSearchArea(lat, lng, radius);
    }

    if (exchange.selectedCode) {
      const targetAmount = exchange.krwAmount || 100000;
      await fetchExchangeEstimate(targetAmount, exchange.selectedCode);
    }
  } catch (error) {
    if (requestId === currentBanksRequestId) {
      console.error('영업점 데이터를 가져오는데 실패했습니다:', error);
      fetchedBanks.value = [];
      updateMapMarkers();
    }
  }
};

const updateMapMarkers = () => {
  markers.forEach(({ marker }) => marker.setMap(null));
  markers = [];

  fetchedBanks.value.forEach((bank) => {
    const position = new window.kakao.maps.LatLng(
      bank.latitude,
      bank.longitude,
    );
    const isSelected = exchange.selectedBankId === bank.id;
    const marker = new window.kakao.maps.Marker({
      position: position,
      title: bank.branchName,
      image: isSelected ? selectedMarkerImage() : unselectedMarkerImage(),
    });
    marker.setMap(map);
    if (isSelected) marker.setZIndex(10);
    markers.push({ id: bank.id, marker });
    window.kakao.maps.event.addListener(marker, 'click', () =>
      selectBank(bank),
    );
  });
};

const refreshMarkerStyles = () => {
  markers.forEach(({ id, marker }) => {
    if (id === exchange.selectedBankId) {
      marker.setImage(selectedMarkerImage());
      marker.setZIndex(10);
    } else {
      marker.setImage(unselectedMarkerImage());
      marker.setZIndex(1);
    }
  });
};

watch(
  () => exchange.selectedBankId,
  () => refreshMarkerStyles(),
);

const searchThisArea = () => {
  if (!map) return;
  const center = map.getCenter();
  const radius = getDynamicRadius();

  // 🎯 isManualSearch = true 플래그 전달
  loadBanks(center.getLat(), center.getLng(), radius, true);
};

const searchLocation = () => {
  if (!query.value.trim()) return;
  const ps = new window.kakao.maps.services.Places();
  ps.keywordSearch(query.value, (data, status) => {
    if (status === window.kakao.maps.services.Status.OK) {
      const bounds = new window.kakao.maps.LatLngBounds();
      for (let i = 0; i < data.length; i++)
        bounds.extend(new window.kakao.maps.LatLng(data[i].y, data[i].x));
      map.setBounds(bounds);
      const center = map.getCenter();
      loadBanks(center.getLat(), center.getLng());
    } else if (status === window.kakao.maps.services.Status.ZERO_RESULT) {
      alert('검색 결과가 존재하지 않습니다.');
    } else {
      alert('검색 중 오류가 발생했습니다. 다시 시도해 주세요.');
    }
  });
};

onUnmounted(() => {
  markers.forEach(({ marker }) => {
    marker.setMap(null);
  });
  markers = [];

  if (currentPositionMarker) {
    currentPositionMarker.setMap(null);
    currentPositionMarker = null;
  }

  // 🎯 검색 반경 원형 제거 추가
  if (searchAreaCircle) {
    searchAreaCircle.setMap(null);
    searchAreaCircle = null;
  }

  map = null;
});

const initMap = (lat, lng) => {
  userLocation.value = { lat, lng };
  map = new window.kakao.maps.Map(mapContainer.value, {
    center: new window.kakao.maps.LatLng(lat, lng),
    level: 4,
  });

  const gpsMarkerImg = new window.kakao.maps.MarkerImage(
    'data:image/svg+xml;charset=utf-8,' +
      encodeURIComponent(
        `<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24">
        <circle cx="12" cy="12" r="6" fill="#FF0000" stroke="#FFFFFF" stroke-width="2"/>
      </svg>`,
      ),
    new window.kakao.maps.Size(24, 24),
    { offset: new window.kakao.maps.Point(12, 12) },
  );

  currentPositionMarker = new window.kakao.maps.Marker({
    position: new window.kakao.maps.LatLng(lat, lng),
    image: gpsMarkerImg,
  });
  currentPositionMarker.setMap(map);

  loadBanks(lat, lng, 2000);

  window.kakao.maps.event.addListener(
    map,
    'dragend',
    () => (showSearchThisAreaBtn.value = true),
  );
  window.kakao.maps.event.addListener(map, 'zoom_changed', () => {
    showSearchThisAreaBtn.value = true;
    handleZoomChanged();
  });
};

onMounted(async () => {
  exchange.selectedBankId = null;
  try {
    const [_, userPos] = await Promise.all([loadKakaoMap(), getUserPosition()]);

    if (!window.kakao || !window.kakao.maps) {
      throw new Error('카카오 지도 객체 생성 실패');
    }

    window.kakao.maps.load(() => {
      initMap(userPos.lat, userPos.lng);
    });
  } catch (error) {
    console.error('카카오 지도 로드 실패:', error);
    mapLoadError.value = true;
  }
});

function selectBank(bank) {
  exchange.selectedBankId = bank.id;
  if (map)
    map.panTo(new window.kakao.maps.LatLng(bank.latitude, bank.longitude));
}

function goToDetail(bank) {
  exchange.selectedBankId = bank.id;
  router.push({
    path: `/exchange/banks/${bank.id}`,
    state: { distance: bank.distance, walk: bank.walk },
  });
}
</script>

<template>
  <div class="nearby-banks-component">
    <label class="search">
      ⌕
      <input
        v-model="query"
        placeholder="지역명 검색 (예: 강남역, 여의도)"
        @keyup.enter="searchLocation"
      />
    </label>

    <section class="map-container-wrapper">
      <div ref="mapContainer" class="kakao-map"></div>

      <!-- ⚡ GPS 로딩 중일 때 표시할 스피너 오버레이 -->
      <div v-if="isGeoLoading" class="geo-loading-overlay">
        <div class="spinner"></div>
        <span>현재 위치 탐색 중...</span>
      </div>

      <div v-if="mapLoadError" class="map-error-overlay">
        <span class="error-icon">⚠️</span>
        <p class="error-msg">지도 서비스를 불러올 수 없습니다.</p>
        <small class="error-sub"
          >네트워크 상태 및 카카오 지도 API 키 설정을 확인해 주세요.</small
        >
      </div>

      <button
        v-if="showSearchThisAreaBtn && !mapLoadError"
        class="search-this-area-btn"
        :class="{ disabled: isTooWide }"
        :disabled="isTooWide"
        @click="searchThisArea"
      >
        {{ isTooWide ? '🔍 지도를 더 확대해 주세요' : '🔍 이 지역 재검색' }}
      </button>

      <button
        v-if="!mapLoadError"
        class="current-btn"
        title="내 위치로"
        @click="moveToCurrentLocation"
      >
        <svg
          width="18"
          height="18"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2.5"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <circle cx="12" cy="12" r="10"></circle>
          <circle cx="12" cy="12" r="2" fill="currentColor"></circle>
          <line x1="12" y1="2" x2="12" y2="5"></line>
          <line x1="12" y1="19" x2="12" y2="22"></line>
          <line x1="2" y1="12" x2="5" y2="12"></line>
          <line x1="19" y1="12" x2="22" y2="12"></line>
        </svg>
      </button>

      <!-- ⚡ GPS 탐색 실패 시 노출되는 가이드 토스트 -->
      <div v-if="isLocationFallback && !isGeoLoading" class="location-notice">
        📍 위치 조회 실패로 기본 위치가 표시됩니다. <b>[내 위치]</b> 버튼을
        눌러주세요.
      </div>
    </section>

    <div class="title">
      <h2>근처 은행</h2>
      <span>반경 {{ displayRadius }} 이내</span>
    </div>

    <section class="list">
      <div
        v-for="bank in sortedBanks"
        :key="bank.id"
        class="list-item"
        :class="{ selected: exchange.selectedBankId === bank.id }"
        @click="selectBank(bank)"
      >
        <i>KB</i>
        <div>
          <b class="branch-name">{{ bank.branchName }}</b>
          <div class="bank-info">
            <small>{{ bank.address }}</small>
            <span class="distance">
              · {{ formatDistance(bank.distance) }}
            </span>
          </div>
        </div>

        <button
          v-if="exchange.selectedBankId === bank.id"
          class="detail-btn"
          @click.stop="goToDetail(bank)"
        >
          상세보기
        </button>
        <em v-else>›</em>
      </div>
      <p v-if="!sortedBanks.length">
        이 지역 반경 {{ displayRadius }} 이내에 국민은행 영업점이 없습니다.
      </p>
    </section>
  </div>
</template>

<style scoped>
.nearby-banks-component {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}
.search {
  display: flex;
  gap: 8px;
  padding: 11px;
  border: 1px solid #e1e6ed;
  border-radius: 11px;
  background: #fff;
  color: #9aa5b5;
  flex-shrink: 0;
  margin-bottom: 11px;
}
.search input {
  flex: 1;
  font-size: 10px;
  outline: none;
}
.map-container-wrapper {
  position: relative;
  height: 230px;
  overflow: hidden;
  border-radius: 14px;
  flex-shrink: 0;
}
.kakao-map {
  width: 100%;
  height: 100%;
  background: #dfeaec;
}

/* ⚡ 위치 탐색 중 오버레이 & 스피너 스타일 */
.geo-loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(255, 255, 255, 0.65);
  backdrop-filter: blur(2px);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  z-index: 15;
  font-size: 11px;
  font-weight: 600;
  color: #1870e8;
}
.spinner {
  width: 22px;
  height: 22px;
  border: 2.5px solid #e1e6ed;
  border-top-color: #1870e8;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* ⚡ 위치 안내 토스트 스타일 */
.location-notice {
  position: absolute;
  bottom: 12px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 10;
  background: rgba(15, 23, 42, 0.85);
  color: #ffffff;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 10px;
  white-space: nowrap;
  pointer-events: none;
}

.search-this-area-btn,
.current-btn {
  z-index: 10;
}
.search-this-area-btn {
  position: absolute;
  top: 12px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 14px;
  border: none;
  border-radius: 20px;
  background: #1870e8;
  color: #fff;
  font-size: 9px;
  font-weight: bold;
  box-shadow: 0 4px 10px rgba(24, 112, 232, 0.3);
  cursor: pointer;
  transition: all 0.2s ease;
}
.search-this-area-btn.disabled {
  background: #a1a1a1;
  color: #f1f1f1;
  box-shadow: none;
  cursor: not-allowed;
}
.current-btn {
  position: absolute;
  right: 16px;
  bottom: 16px;
  z-index: 2;
  display: flex;
  width: 40px;
  height: 40px;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 50%;
  background: #fff;
  color: #1672ed;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  cursor: pointer;
}
.title {
  display: flex;
  justify-content: space-between;
  margin: 18px 2px 8px;
  flex-shrink: 0;
}
.title h2 {
  font-size: 12px;
}
.title span {
  color: #718097;
  font-size: 8px;
}
.list {
  flex: 1;
  overflow-y: auto;
  padding-bottom: 20px;
}
.list-item {
  display: grid;
  width: 100%;
  grid-template-columns: 34px 1fr auto;
  align-items: center;
  gap: 9px;
  margin-top: 8px;
  padding: 12px;
  border: 1px solid #e1e6ed;
  border-radius: 12px;
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s ease;
}
.list-item.selected {
  border-color: #8ebaf4;
  background: #f0f6ff;
}
.list i {
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  border-radius: 9px;
  background: #fff4d2;
  color: #c98400;
  font-size: 8px;
  font-style: normal;
  font-weight: 900;
}
.list-item b.branch-name {
  font-size: 11px;
}
.bank-info {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 4px;
}
.bank-info small {
  margin-top: 0;
  color: #8995a6;
  font-size: 10px;
}
.distance {
  color: #1870e8;
  font-weight: 600;
  font-size: 10px;
}
.detail-btn {
  padding: 6px 12px;
  border: none;
  border-radius: 20px;
  background: #1870e8;
  color: #fff;
  font-size: 9px;
  font-weight: bold;
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(24, 112, 232, 0.2);
}
.list em {
  color: #9da7b5;
  font-size: 18px;
  font-style: normal;
  padding: 0 4px;
}
.list p {
  padding: 40px;
  text-align: center;
  color: #94a3b8;
  font-size: 9px;
}
.map-error-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(240, 243, 246, 0.95);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 100;
  text-align: center;
  padding: 20px;
}
.error-icon {
  font-size: 30px;
  margin-bottom: 8px;
}
.error-msg {
  font-size: 13px;
  font-weight: bold;
  color: #10192d;
  margin: 0;
}
.error-sub {
  font-size: 10px;
  color: #64748b;
  margin-top: 4px;
}
</style>
