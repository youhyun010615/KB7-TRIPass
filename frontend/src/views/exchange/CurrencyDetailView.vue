<script setup>
import { computed, ref, watch, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import BottomNav from '@/components/common/BottomNav.vue';
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

const route = useRoute();
const router = useRouter();
const exchange = useExchangeStore();
const currency = computed(() => exchange.getCurrency(route.params.code));
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
    },
  ],
});

const chartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { display: true },
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

const fetchHistory = async () => {
  if (!currency.value) return;
  const days = periods.find((p) => p[0] === exchange.period)?.[2] || 7;
  try {
    const data = await fetchExchangeRatesHistory(currency.value.code, days);
    console.log(data);
    history.value = data.rates;

    // Update chartData
    chartData.value = {
      labels: history.value.map((item) => {
        const date = new Date(item.rateDate);
        return `${date.getMonth() + 1}/${date.getDate()}`;
      }),
      datasets: [
        {
          ...chartData.value.datasets[0],
          data: history.value.map((item) => item.dealBaseRate),
        },
      ],
    };
  } catch (e) {
    console.error('Failed to fetch history', e);
  }
};

watch(() => exchange.period, fetchHistory);
onMounted(fetchHistory);
</script>

<template>
  <main class="page">
    <div class="shell">
      <header>
        <button @click="router.back()">‹</button>
        <h1>{{ currency?.name }} 환율</h1>
      </header>
      <template v-if="currency">
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
            최근 {{ periods.find((p) => p[0] === exchange.period)?.[1] }} 환율
            추이
          </h2>
          <strong>{{ format(currency.rate) }}<small>원</small></strong>
          <p :class="{ up: currency.change > 0 }">
            {{ currency.change > 0 ? '▲' : '▼' }}
            {{ format(Math.abs(currency.change)) }}원
          </p>
          <div class="chart-container" style="height: 200px">
            <Line :data="chartData" :options="chartOptions" />
          </div>
        </section>
        <button
          class="alert"
          @click="router.push(`/exchange/alerts/new?code=${currency.code}`)"
        >
          목표 환율 알림 등록
        </button>
      </template>
      <BottomNav />
    </div>
  </main>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #e7ecf4;
  color: #10192d;
}
.shell {
  width: min(100%, 390px);
  min-height: 100vh;
  margin: auto;
  padding: 52px 18px 100px;
  background: #f7f5ef;
}
header {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}
header button {
  width: 28px;
  font-size: 25px;
}
header h1 {
  flex: 1;
  padding-right: 28px;
  text-align: center;
  font-size: 18px;
}
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
.chart,
.compare {
  margin-top: 12px;
  padding: 15px;
  border: 1px solid #e1e6ed;
  border-radius: 15px;
  background: #fff;
}
.chart h2,
.compare h2 {
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
  color: #0a9d73;
  font-size: 8px;
}
.chart p.up {
  color: #dd665a;
}
.chart-container {
  width: 100%;
  margin-top: 10px;
}
.compare div {
  display: grid;
  grid-template-columns: 1fr auto 35px;
  padding: 10px 0;
  border-bottom: 1px solid #eef1f5;
  font-size: 9px;
}
.compare em {
  margin-left: 5px;
  color: #08a477;
  font-size: 7px;
  font-style: normal;
}
.alert {
  width: 100%;
  margin-top: 13px;
  padding: 14px;
  border-radius: 11px;
  background: #173f8d;
  color: #fff;
  font-weight: 900;
}
.shell :deep(.fixed) {
  display: flex;
  gap: 0;
  margin-top: 0;
}
</style>
