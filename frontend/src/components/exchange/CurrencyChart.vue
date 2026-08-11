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
    legend: { display: true },
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
  <section class="chart">
    <h2>
      최근 {{ periods.find((p) => p[0] === exchange.period)?.[1] }} 환율 추이
    </h2>
    <strong
      >{{ safeCurrency.unit }}{{ safeCurrency.symbol }} =
      {{ format(safeCurrency.rate) }}<small>원</small></strong
    >
    <p :class="{ up: safeCurrency.change > 0, down: safeCurrency.change <= 0 }">
      <span class="arrow">{{ safeCurrency.change > 0 ? '▲' : '▼' }}</span>
      {{ format(Math.abs(safeCurrency.change)) }}원
    </p>
    <div class="chart-container" style="height: 200px">
      <Line :data="chartData" :options="chartOptions" />
    </div>
  </section>
</template>

<style scoped>
nav {
  display: flex;
  gap: 7px;
  margin-top: 13px;
}
nav button {
  padding: 8px 13px;
  border-radius: 14px;
  background: #fff;
  color: #738096;
  font-size: 8px;
}
nav .active {
  background: #176ff2;
  color: #fff;
}
.chart {
  margin-top: 12px;
  padding: 15px;
  border: 1px solid #e1e6ed;
  border-radius: 15px;
  background: #fff;
}
.chart h2 {
  font-size: 11px;
}
.chart strong {
  display: block;
  margin-top: 10px;
  color: #174494;
  font-size: 22px;
}
.chart strong small {
  font-size: 9px;
}
.chart p {
  font-size: 8px;
}
.chart p.up {
  color: #dd665a;
}
.chart p.down {
  color: #1b2dd3;
}
.chart .arrow {
  margin-right: 2px;
}
</style>
