<script setup>
import { ref, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useExchangeStore } from '@/stores/exchange';
import { fetchNearbyBanks } from '@/api/exchange';

const router = useRouter();
const exchange = useExchangeStore();
const query = ref('');
const fetchedBanks = ref([]); // 실시간 영업점 목록

const mapContainer = ref(null);
let map = null;
let markers = []; // 지도 마커 객체들을 { id, marker } 구조로 보관
let currentPositionMarker = null; // 내 위치 마커

// '이 지역 재검색' 버튼 표시 여부
const showSearchThisAreaBtn = ref(false);

// 커스텀 SVG 마커 생성 헬퍼 함수 (KB 브랜드 컬러)
const getSvgIconUri = (fillColor, centerColor, isLarge = false) => {
  const width = isLarge ? 18 : 14;
  const height = isLarge ? 24 : 19;
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 24 32">
    <path d="M12 0C5.37 0 0 5.37 0 12c0 9.3 12 20 12 20s12-10.7 12-20c0-6.63-5.37-12-12-12z" fill="${fillColor}"/>
    <circle cx="12" cy="12" r="4.5" fill="${centerColor}"/>
  </svg>`;
  return 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svg);
};

// 미선택 마커 이미지 (회색 핀 + KB 노란색 센터) - 절반 크기로 미니멀하게 축소
const unselectedMarkerImage = () => {
  return new window.kakao.maps.MarkerImage(
    getSvgIconUri('#4B433F', '#FFCC00', false),
    new window.kakao.maps.Size(14, 19),
    { offset: new window.kakao.maps.Point(7, 19) }, // 핀 하단 중앙 오프셋 (14/2, 19)
  );
};

// 선택된 마커 이미지 (KB 노란색 핀 + 회색 센터) - 절반 크기로 축소
const selectedMarkerImage = () => {
  return new window.kakao.maps.MarkerImage(
    getSvgIconUri('#FFCC00', '#4B433F', true),
    new window.kakao.maps.Size(18, 24),
    { offset: new window.kakao.maps.Point(9, 24) }, // 핀 하단 중앙 오프셋 (18/2, 24)
  );
};

// 카카오 맵 SDK 로드
const loadKakaoMap = () => {
  return new Promise((resolve) => {
    const script = document.createElement('script');
    script.src = `//dapi.kakao.com/v2/maps/sdk.js?appkey=${import.meta.env.VITE_KAKAO_MAP_KEY}&autoload=false&libraries=services`;
    script.onload = () => resolve();
    document.head.appendChild(script);
  });
};

// 백엔드 API로부터 은행 데이터 로드 및 마커 표시
const loadBanks = async (lat, lng) => {
  try {
    const radius = 2000; // 반경 2km
    const data = await fetchNearbyBanks(lat, lng, radius);
    fetchedBanks.value = data || [];
    updateMapMarkers();
    showSearchThisAreaBtn.value = false; // 검색 완료 시 버튼 숨김
  } catch (error) {
    console.error('영업점 데이터를 가져오는데 실패했습니다:', error);
  }
};

// 지도 마커들 새로 그리기 및 매핑 저장
const updateMapMarkers = () => {
  // 기존 마커 전체 제거
  markers.forEach(({ marker }) => marker.setMap(null));
  markers = [];

  // 새 영업점 마커 표시
  fetchedBanks.value.forEach((bank) => {
    const position = new window.kakao.maps.LatLng(
      bank.latitude,
      bank.longitude,
    );

    // 현재 선택된 은행인지 판별하여 최초 이미지 지정
    const isSelected = exchange.selectedBankId === bank.id;
    const markerImg = isSelected
      ? selectedMarkerImage()
      : unselectedMarkerImage();

    const marker = new window.kakao.maps.Marker({
      position: position,
      title: bank.branchName,
      image: markerImg,
    });

    marker.setMap(map);

    // Z-Index 설정 (선택된 마커가 더 위에 오도록 처리)
    if (isSelected) marker.setZIndex(10);

    // 나중에 반응형으로 교체하기 위해 배열에 마커 정보 매핑 저장
    markers.push({ id: bank.id, marker });

    // 마커 클릭 시 영업점 포커싱 실행
    window.kakao.maps.event.addListener(marker, 'click', () => {
      selectBank(bank);
    });
  });
};

// 선택된 은행이 바뀔 때 마커 이미지만 실시간 교체하는 로직
const refreshMarkerStyles = () => {
  markers.forEach(({ id, marker }) => {
    if (id === exchange.selectedBankId) {
      marker.setImage(selectedMarkerImage());
      marker.setZIndex(10); // 선택된 것을 맨 위로 올림
    } else {
      marker.setImage(unselectedMarkerImage());
      marker.setZIndex(1); // 미선택은 기본 레이어 레벨
    }
  });
};

// Pinia Store의 선택 은행 ID 변화 감시하여 마커 스타일 반응형 변경
watch(
  () => exchange.selectedBankId,
  () => {
    refreshMarkerStyles();
  },
);

// 내 위치로 화면 이동 및 즉시 자동 조회
const moveToCurrentLocation = () => {
  if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition((position) => {
      const lat = position.coords.latitude;
      const lng = position.coords.longitude;
      const moveLatLon = new window.kakao.maps.LatLng(lat, lng);

      map.panTo(moveLatLon); // 부드럽게 이동

      // 내 위치 빨간 마커 위치 갱신
      if (currentPositionMarker) {
        currentPositionMarker.setPosition(moveLatLon);
      }

      loadBanks(lat, lng); // 즉시 갱신
    });
  }
};

// "이 지역 재검색" 클릭 시 작동
const searchThisArea = () => {
  if (!map) return;
  const center = map.getCenter();
  loadBanks(center.getLat(), center.getLng());
};

// 검색창 입력으로 지역 이동 및 즉시 자동 조회
const searchLocation = () => {
  if (!query.value.trim()) return;

  const ps = new window.kakao.maps.services.Places();
  ps.keywordSearch(query.value, (data, status) => {
    if (status === window.kakao.maps.services.Status.OK) {
      const bounds = new window.kakao.maps.LatLngBounds();
      for (let i = 0; i < data.length; i++) {
        bounds.extend(new window.kakao.maps.LatLng(data[i].y, data[i].x));
      }
      map.setBounds(bounds); // 해당 영역으로 지도 축척 및 이동

      // 이동한 중심점 기준 바로 실시간 은행 갱신
      const center = map.getCenter();
      loadBanks(center.getLat(), center.getLng());
    } else {
      alert('검색 결과가 없습니다. 지역명을 정확히 입력해 주세요.');
    }
  });
};

// 지도 초기화 및 바인딩
const initMap = (lat, lng) => {
  const options = {
    center: new window.kakao.maps.LatLng(lat, lng),
    level: 4,
    draggable: true, // 명시적으로 드래그 활성화
    zoomable: true, // 명시적으로 휠 줌 활성화
  };
  map = new window.kakao.maps.Map(mapContainer.value, options);

  // 내 GPS 위치 마커 (파란색 레이더 스타일 SVG 이미지 적용)
  const gpsSvg = `<svg xmlns="http://www.w3.org/2000/svg" width="30" height="30" viewBox="0 0 30 30">
    <circle cx="15" cy="15" r="9" fill="#1870e8" stroke="#ffffff" stroke-width="3"/>
    <circle cx="15" cy="15" r="3" fill="#ffffff"/>
  </svg>`;
  const gpsMarkerImg = new window.kakao.maps.MarkerImage(
    'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(gpsSvg),
    new window.kakao.maps.Size(30, 30),
    { offset: new window.kakao.maps.Point(15, 15) },
  );

  const locPosition = new window.kakao.maps.LatLng(lat, lng);
  currentPositionMarker = new window.kakao.maps.Marker({
    position: locPosition,
    image: gpsMarkerImg,
  });
  currentPositionMarker.setMap(map);

  // 초기 현재 위치 주변 은행 데이터 즉시 호출
  loadBanks(lat, lng);

  // 지도가 드래그되거나 확대/축소되었을 때만 '이 지역 재검색' 버튼 띄우기
  window.kakao.maps.event.addListener(map, 'dragend', () => {
    showSearchThisAreaBtn.value = true;
  });
  window.kakao.maps.event.addListener(map, 'zoom_changed', () => {
    showSearchThisAreaBtn.value = true;
  });
};

onMounted(async () => {
  // 진입 시 이전 선택 상태 완전히 초기화 (깨끗한 첫 상태 제공)
  exchange.selectedBankId = null;

  await loadKakaoMap();

  window.kakao.maps.load(() => {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          initMap(position.coords.latitude, position.coords.longitude);
        },
        () => {
          initMap(37.497942, 127.027621); // 거부 시 강남역
        },
      );
    } else {
      initMap(37.497942, 127.027621);
    }
  });
});

// 영업점 선택 및 지도 이동 (이동 및 선택만 수행, 상세 이동은 버튼으로 위임)
function selectBank(bank) {
  exchange.selectedBankId = bank.id;

  if (map) {
    const position = new window.kakao.maps.LatLng(
      bank.latitude,
      bank.longitude,
    );
    map.panTo(position); // 부드럽게 지도의 포커스를 해당 마커로 이동
  }
}

// 화살표(Chevron)나 상세보기 버튼 클릭 시 상세화면 이동
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

    <!-- 지도 영역 -->
    <section class="map-container-wrapper">
      <div ref="mapContainer" class="kakao-map"></div>

      <!-- 플로팅 버튼들 -->
      <button
        v-if="showSearchThisAreaBtn"
        class="search-this-area-btn"
        @click="searchThisArea"
      >
        🔍 이 지역 재검색
      </button>

      <button
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
      <span>반경 2km 이내</span>
    </div>

    <!-- 스크롤 가능한 목록 영역 -->
    <section class="list">
      <div
        v-for="bank in fetchedBanks"
        :key="bank.id"
        class="list-item"
        :class="{ selected: exchange.selectedBankId === bank.id }"
        @click="selectBank(bank)"
      >
        <i>KB</i>
        <div>
          <b>{{ bank.branchName }}</b>
          <small>{{ bank.address }}</small>
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
      <p v-if="!fetchedBanks.length">
        이 지역 반경 2km 이내에 국민은행 영업점이 없습니다.
      </p>
    </section>
  </div>
</template>

<style scoped>
.nearby-banks-component {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden; /* 컨테이너 외부 스크롤 방지 */
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
  font-size: 8px;
  outline: none;
}

/* 지도 영역 - 고정 높이 */
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

/* 지도 내 버튼들 (절대 위치) */
.search-this-area-btn,
.current-btn {
  z-index: 10;
}

/* 이 지역 재검색 플로팅 버튼 */
.search-this-area-btn {
  position: absolute;
  top: 12px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 10;
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
.search-this-area-btn:hover {
  background: #145ec7;
}
.search-this-area-btn:active {
  transform: translateX(-50%) scale(0.95);
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
  transition: all 0.2s ease;
}
.current-btn:active {
  transform: scale(0.95);
  background: #f8f9fa;
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

/* 카드 목록 레이아웃 개편 (div 구조 적용) */
.list {
  flex: 1;
  overflow-y: auto;
  padding-bottom: 20px;
}
.list-item {
  display: grid;
  width: 100%;
  grid-template-columns: 34px 1fr auto; /* 우측 상세보기 버튼을 위해 auto로 변경 */
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
  background: #f0f6ff; /* 선택되었을 때 배경을 미세하게 푸른빛으로 강조 */
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
.list b,
.list small {
  display: block;
}
.list b {
  font-size: 9px;
}
.list small {
  margin-top: 4px;
  color: #8995a6;
  font-size: 7px;
}

/* 우측 상세보기 버튼 스타일 */
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
  transition: all 0.2s ease;
}
.detail-btn:active {
  transform: scale(0.95);
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
</style>
