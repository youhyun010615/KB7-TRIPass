<script setup>
import { computed, ref, watch, onMounted } from 'vue';
import { useExchangeStore } from '@/stores/exchange';
import { fetchExchangeRatesHistory } from '@/api/exchange';

// Chart.js imports
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
} from 'chart.js';
import { Line } from 'vue-chartjs';

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
);

const props = defineProps({
  currency: Object,
  lastUpdateDate: String,
});

const safeCurrency = computed(() => props.currency || {});
const exchange = useExchangeStore();
const history = ref([]);

// New chart data structure
const chartData = ref({
  labels: [],
  datasets: [
    {
      label: '환율',
      data: [],
      borderColor: '#176ff2',
      backgroundColor: 'rgba(23, 111, 242, 0.1)',
      tension: 0.1,
      pointRadius: 3,
      pointHoverRadius: 6,
    },
  ],
});

const chartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  interaction: {
    mode: 'index',
    intersect: false,
  },
  plugins: {
    legend: { display: true, position: 'top', align: 'end' },
    tooltip: {
      enabled: true,
      callbacks: {
        label: (context) => {
          let label = context.dataset.label || '';
          if (label) {
            label += ': ';
          }
          if (context.parsed.y !== null) {
            label += format(context.parsed.y) + '원';
          }
          return label;
        },
      },
    },
  },
  scales: {
    y: { beginAtZero: false },
  },
};

const periods = [
  ['1w', '1주', 7],
  ['1m', '1개월', 30],
  ['3m', '3개월', 90],
];
const format = (v) =>
  Number(v || 0).toLocaleString('ko-KR', { maximumFractionDigits: 2 });

let currentRequestId = 0;

const fetchHistory = async () => {
  if (!props.currency?.code) return;
  const days = periods.find((p) => p[0] === exchange.period)?.[2] || 7;
  const requestId = ++currentRequestId;

  try {
    const data = await fetchExchangeRatesHistory(props.currency.code, days);

    // 이전 요청의 결과는 무시
    if (requestId !== currentRequestId) return;

    const rates = data?.rates || [];
    history.value = rates;

    const unit = props.currency?.unit || 1;

    // Update chartData
    chartData.value = {
      labels: rates.map((item) => {
        const date = new Date(item.rateDate);
        return `${date.getMonth() + 1}/${date.getDate()}`;
      }),
      datasets: [
        {
          ...chartData.value.datasets[0],
          data: rates.map((item) => item.dealBaseRate * unit),
        },
      ],
    };
  } catch (e) {
    if (requestId === currentRequestId) {
      console.error('Failed to fetch history', e);
      // 에러 발생 시 차트 및 히스토리 초기화로 이전 데이터 보존 방지
      history.value = [];
      chartData.value = {
        labels: [],
        datasets: [
          {
            ...chartData.value.datasets[0],
            data: [],
          },
        ],
      };
    }
  }
};

watch(() => exchange.period, fetchHistory);
watch(() => props.currency, fetchHistory, { immediate: true });
</script>

<template v-if="props.currency">
  <section class="chart">
    <div class="chart-head">
      <h2>환율 추이</h2>
      <nav>
        <button
          v-for="p in periods"
          :key="p[0]"
          :class="{ active: exchange.period === p[0] }"
          @click="exchange.period = p[0]"
        >
          {{ p[1] }}
        </button>
      </nav>
    </div>
    <small v-if="lastUpdateDate" class="update-info">
      {{ lastUpdateDate }} 고시 기준
      <span class="update-note">· 매일 오전 11시 갱신</span>
    </small>
    <div class="rate-row">
      <strong
        >{{ safeCurrency.unit }}{{ safeCurrency.symbol }} =
        {{ format(safeCurrency.rate) }}<small>원</small></strong
      >
      <p class="change-row" :class="{ up: safeCurrency.change > 0, down: safeCurrency.change <= 0 }">
        <small>전일 대비</small>
        <span class="arrow">{{ safeCurrency.change > 0 ? '▲' : '▼' }}</span>
        {{ format(Math.abs(safeCurrency.change)) }}원
      </p>
    </div>
    <div class="chart-container" style="height: 148px">
      <Line :data="chartData" :options="chartOptions" />
    </div>
  </section>
</template>

<style scoped>
.chart-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
nav {
  display: grid;
  flex: none;
  grid-template-columns: repeat(3, 1fr);
  gap: 0;
  padding: 3px;
  border-radius: 999px;
  background: #e9ecf3;
}
nav button {
  min-height: 24px;
  padding: 4px 8px;
  border-radius: 999px;
  background: transparent;
  color: #929caf;
  font-size: 10.5px;
  font-weight: 700;
}
nav .active {
  background: #123478;
  color: #fff;
  box-shadow: 0 5px 12px rgba(18, 52, 120, .17);
}
.chart {
  margin-top: 18px;
  padding: 22px 18px 18px;
  border: 1px solid #bcd2ff;
  border-radius: 22px;
  background: linear-gradient(150deg, #eff5ff 0%, #e6f0ff 100%);
  box-shadow: 0 10px 26px rgba(23, 43, 77, .055);
}
.chart h2 {
  color: #173f8d;
  font-size: 15px;
  font-weight: 800;
}
.chart strong {
  display: block;
  margin-top: 12px;
  color: #10192d;
  font-size: 21px;
  font-weight: 800;
  letter-spacing: .02em;
  white-space: nowrap;
}
.rate-row {
  display: flex;
  align-items: flex-start;
  flex-direction: column;
  gap: 0;
}
.update-info {
  display: block;
  margin-top: 6px;
  margin-bottom: 8px;
  color: #9aa6b8;
  font-size: 9px;
  font-weight: 500;
  white-space: nowrap;
}
.update-note {
  color: #b7c0cf;
}
.chart strong small {
  font-size: 11.5px;
}
.chart p {
  margin-top: 2px;
  font-size: 12px;
  font-weight: 700;
}
.chart .change-row {
  display: flex;
  align-items: center;
  gap: 3px;
  line-height: 1.25;
}
.chart .change-row small {
  color: #9aa6b8;
  font-size: 8.5px;
  font-weight: 600;
}
.chart p.up {
  color: #ed5555;
}
.chart p.down {
  color: #3972d8;
}
.chart .arrow {
  margin-right: 2px;
}
</style>
