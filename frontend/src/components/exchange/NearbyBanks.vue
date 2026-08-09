<script setup>
import { ref, onMounted, watch, computed, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { useExchangeStore } from '@/stores/exchange';
import { fetchNearbyBanks } from '@/api/exchange';

const router = useRouter();
const exchange = useExchangeStore();
const query = ref('');
const fetchedBanks = ref([]); // 실시간 영업점 목록
const currentRadius = ref(2000);
const userLocation = ref(null); // 사용자의 실제 GPS 원본 위치 저장용

// 두 지점 간의 거리 계산 (Haversine 공식)
const getDistance = (lat1, lng1, lat2, lng2) => {
  const R = 6371e3; // 지구 반경 (m)
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLng = ((lng2 - lng1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos((lat1 * Math.PI) / 180) *
      Math.cos((lat2 * Math.PI) / 180) *
      Math.sin(dLng / 2) *
      Math.sin(dLng / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c; // 미터 단위 거리
};

// 현재 지도의 남서쪽 모서리와 중심 간의 거리를 구하여 동적으로 검색 반경 설정
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
  // 최소 1km, 최대 20km로 제한하여 쿼리 최적화
  return Math.min(20000, Math.max(1000, radius));
};

const displayRadius = computed(() => {
  const r = currentRadius.value;
  if (r >= 1000) {
    return `${(r / 1000).toFixed(1)}km`;
  }
  return `${Math.round(r)}m`;
});

// 거리 기준으로 정렬된 은행 목록
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
  const width = isLarge ? 18 : 14;
  const height = isLarge ? 24 : 19;
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 24 32">
    <path d="M12 0C5.37 0 0 5.37 0 12c0 9.3 12 20 12 20s12-10.7 12-20c0-6.63-5.37-12-12-12z" fill="${fillColor}"/>
    <circle cx="12" cy="12" r="4.5" fill="${centerColor}"/>
  </svg>`;
  return 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svg);
};

const unselectedMarkerImage = () => {
  return new window.kakao.maps.MarkerImage(
    getSvgIconUri('#4B433F', '#FFCC00', false),
    new window.kakao.maps.Size(14, 19),
    { offset: new window.kakao.maps.Point(7, 19) },
  );
};

const selectedMarkerImage = () => {
  return new window.kakao.maps.MarkerImage(
    getSvgIconUri('#FFCC00', '#4B433F', true),
    new window.kakao.maps.Size(18, 24),
    { offset: new window.kakao.maps.Point(9, 24) },
  );
};

const mapLoadError = ref(false);

const loadKakaoMap = () => {
  return new Promise((resolve, reject) => {
    // 1. 이미 정상 로드된 경우 즉시 완료
    if (window.kakao && window.kakao.maps) {
      resolve();
      return;
    }

    // 7초 타임아웃 설정 (영구 대기 방지)
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
    script.src = `//dapi.kakao.com/v2/maps/sdk.js?appkey=${import.meta.env.VITE_KAKAO_MAP_KEY}&autoload=false&libraries=services`;
    
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

const loadBanks = async (lat, lng, searchRadius = null) => {
  const requestId = ++currentBanksRequestId;
  try {
    const radius = searchRadius || getDynamicRadius();
    currentRadius.value = radius;
    const data = await fetchNearbyBanks(lat, lng, radius);
    
    // 이전 요청의 결과는 무시 (레이스 컨디션 방어)
    if (requestId !== currentBanksRequestId) return;

    fetchedBanks.value = (data || []).map((bank) => {
      // 실제 유저의 물리적 원본 GPS 위치가 존재하면 그 기준 좌표로 거리를 재계산하여 표시
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

const moveToCurrentLocation = () => {
  if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(
      (position) => {
        const lat = position.coords.latitude;
        const lng = position.coords.longitude;
        userLocation.value = { lat, lng }; // 실제 물리 유저 GPS 위치 저장
        map.panTo(new window.kakao.maps.LatLng(lat, lng));
        if (currentPositionMarker)
          currentPositionMarker.setPosition(
            new window.kakao.maps.LatLng(lat, lng),
          );
        loadBanks(lat, lng);
      },
      (error) => {
        console.warn('Geolocation error:', error);
      },
      { enableHighAccuracy: false, timeout: 3000, maximumAge: 60000 },
    );
  }
};

const searchThisArea = () => {
  if (!map) return;
  const center = map.getCenter();
  const radius = getDynamicRadius();
  loadBanks(center.getLat(), center.getLng(), radius);
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
  // 모든 마커의 맵 연결 해제 및 리스너 해제 유도
  markers.forEach(({ marker }) => {
    marker.setMap(null);
  });
  markers = [];

  if (currentPositionMarker) {
    currentPositionMarker.setMap(null);
    currentPositionMarker = null;
  }

  // 지도 객체 해제
  map = null;
});

const initMap = (lat, lng) => {
  userLocation.value = { lat, lng }; // 실제 물리 유저 GPS 위치 저장
  map = new window.kakao.maps.Map(mapContainer.value, {
    center: new window.kakao.maps.LatLng(lat, lng),
    level: 4,
  });
  const gpsMarkerImg = new window.kakao.maps.MarkerImage(
    'data:image/svg+xml;charset=utf-8,' +
      encodeURIComponent(
        `<svg xmlns="http://www.w3.org/2000/svg" width="30" height="30" viewBox="0 0 30 30"><circle cx="15" cy="15" r="9" fill="#1870e8" stroke="#ffffff" stroke-width="3"/><circle cx="15" cy="15" r="3" fill="#ffffff"/></svg>`,
      ),
    new window.kakao.maps.Size(30, 30),
    { offset: new window.kakao.maps.Point(15, 15) },
  );
  currentPositionMarker = new window.kakao.maps.Marker({
    position: new window.kakao.maps.LatLng(lat, lng),
    image: gpsMarkerImg,
  });
  currentPositionMarker.setMap(map);
  loadBanks(lat, lng, 2000); // 초기 진입 시에는 기본 2km 반경 탐색
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
    await loadKakaoMap();
    if (!window.kakao || !window.kakao.maps) {
      throw new Error('카카오 지도 객체 생성 실패');
    }
    window.kakao.maps.load(() => {
      // 1. 기본 위치(강남역)로 지도를 대기 없이 즉시 렌더링 (체감 로딩 속도 0초!)
      initMap(37.497942, 127.027621);

      // 2. 백그라운드에서 유저의 실제 GPS를 가져와 성공 시 해당 위치로 슬라이드(panTo) 및 은행 정보 갱신
      if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(
          (position) => {
            const lat = position.coords.latitude;
            const lng = position.coords.longitude;
            userLocation.value = { lat, lng };
            if (map) {
              map.panTo(new window.kakao.maps.LatLng(lat, lng));
              if (currentPositionMarker) {
                currentPositionMarker.setPosition(
                  new window.kakao.maps.LatLng(lat, lng),
                );
              }
              loadBanks(lat, lng);
            }
          },
          (error) => {
            console.warn('Geolocation background fetch error:', error);
          },
          { enableHighAccuracy: false, timeout: 3000, maximumAge: 60000 },
        );
      }
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
      <div v-if="mapLoadError" class="map-error-overlay">
        <span class="error-icon">⚠️</span>
        <p class="error-msg">지도 서비스를 불러올 수 없습니다.</p>
        <small class="error-sub">네트워크 상태 및 카카오 지도 API 키 설정을 확인해 주세요.</small>
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
              ·
              {{
                bank.distance < 1000
                  ? `${Math.round(bank.distance)}m`
                  : `${(bank.distance / 1000).toFixed(1)}km`
              }}
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
