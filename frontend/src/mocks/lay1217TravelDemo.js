import { todayIso } from '@/utils/devDate'

const TARGET = 3_338_000
const INITIAL_WALLET = 3_938_008

const countryData = {
  프랑스: {
    budget: 1_228_000,
    day9: { total: 1_045_440, categories: [154_960, 336_480, 58_320, 126_360, 34_020, 308_300, 27_000] },
    day13: { total: 1_045_440, categories: [154_960, 336_480, 58_320, 126_360, 34_020, 308_300, 27_000] },
    end: { total: 1_045_440, categories: [154_960, 336_480, 58_320, 126_360, 34_020, 308_300, 27_000] },
  },
  스위스: {
    budget: 1_170_000,
    day9: { total: 643_600, categories: [29_500, 208_200, 28_480, 171_020, 21_360, 167_240, 17_800] },
    day13: { total: 1_083_620, categories: [109_500, 324_200, 46_480, 283_020, 46_360, 243_260, 30_800] },
    end: { total: 1_083_620, categories: [109_500, 324_200, 46_480, 283_020, 46_360, 243_260, 30_800] },
  },
  포르투갈: {
    budget: 940_000,
    day9: { total: 0, categories: [0, 0, 0, 0, 0, 0, 0] },
    day13: { total: 1_232_500, categories: [301_100, 254_500, 47_300, 158_600, 29_700, 405_100, 36_200] },
    end: { total: 1_342_560, categories: [301_100, 296_500, 58_800, 177_400, 29_700, 430_100, 48_960] },
  },
}

const categoryNames = ['쇼핑', '식비', '카페', '교통', '생활비', '취미여가', '기타']
const countryIdNames = new Map()
const transactionCache = new Map()
const categoryIds = { 식비: 1, 교통: 2, 쇼핑: 4, 관광: 5, 기타: 6, 카페: 7, 생활비: 8, 취미여가: 9 }
const merchantNames = {
  스위스: {
    쇼핑: ['Coop City Zürich', 'Victorinox Flagship Store', 'Läderach Zürich'],
    식비: ['Zeughauskeller', 'Swiss Chuchi Restaurant', 'Hiltl Zürich'],
    카페: ['Confiserie Sprüngli', 'Café Schober', 'Bäckerei Jung'],
    교통: ['SBB Mobile', 'Zürich HB 교통권', 'Jungfraubahn'],
    생활비: ['Migros Zürich', 'Coop Supermarkt'],
    취미여가: ['Kunsthaus Zürich', 'Jungfraujoch 전망대', '루체른 호수 크루즈'],
    기타: ['Zürich HB 보관함', '관광 안내소'],
  },
  프랑스: {
    쇼핑: ['Galeries Lafayette', 'Monoprix Paris'], 식비: ['Bouillon Chartier', 'Le Comptoir'],
    카페: ['Café de Flore', 'Angelina Paris'], 교통: ['Île-de-France Mobilités', 'SNCF Connect'],
    생활비: ['Carrefour City'], 취미여가: ['Musée du Louvre', 'Bateaux Mouches'], 기타: ['Paris consigne'],
  },
  포르투갈: {
    쇼핑: ['A Vida Portuguesa', 'El Corte Inglés Lisboa'], 식비: ['Time Out Market', 'Cervejaria Ramiro'],
    카페: ['Pastéis de Belém', 'Fábrica Coffee Roasters'], 교통: ['Viva Viagem', 'Comboios de Portugal'],
    생활비: ['Pingo Doce'], 취미여가: ['Palácio da Pena', 'Lisbon Oceanarium'], 기타: ['Lisboa Lockers'],
  },
}

const daily = [
  ['2027-04-04', '프랑스', 124_740], ['2027-04-05', '프랑스', 238_140],
  ['2027-04-06', '프랑스', 146_880], ['2027-04-07', '프랑스', 193_860],
  ['2027-04-08', '프랑스', 187_920], ['2027-04-09', '프랑스', 153_900],
  ['2027-04-09', '스위스', 122_820], ['2027-04-10', '스위스', 186_900],
  ['2027-04-11', '스위스', 141_240], ['2027-04-12', '스위스', 192_640],
  ['2027-04-13', '스위스', 228_460], ['2027-04-14', '스위스', 211_560],
  ['2027-04-14', '포르투갈', 238_500], ['2027-04-15', '포르투갈', 331_200],
  ['2027-04-16', '포르투갈', 662_800], ['2027-04-17', '포르투갈', 68_260],
  ['2027-04-18', '포르투갈', 41_800],
]

const demoSchedules = [
  ['fr-0405', '프랑스', 'FR', '2027-04-05T10:00:00', '루브르 박물관 가이드 투어', '루브르 박물관'],
  ['fr-0406', '프랑스', 'FR', '2027-04-06T09:30:00', '베르사유 궁전 투어', '베르사유 궁전'],
  ['fr-0407', '프랑스', 'FR', '2027-04-07T19:30:00', '센강 선셋 크루즈', '센강'],
  ['fr-0408', '프랑스', 'FR', '2027-04-08T14:00:00', '몽마르트·사크레쾌르 산책', '몽마르트'],
  ['fr-0409', '프랑스', 'FR', '2027-04-09T09:00:00', '리옹역 출발·스위스 이동', '파리 리옹역'],
  ['ch-0409', '스위스', 'CH', '2027-04-09T16:00:00', '루체른 도착·호텔 체크인', '루체른'],
  ['ch-0410', '스위스', 'CH', '2027-04-10T09:00:00', '융프라우요흐 투어', '융프라우요흐'],
  ['ch-0411', '스위스', 'CH', '2027-04-11T10:00:00', '그린델발트 퍼스트', '그린델발트'],
  ['ch-0412', '스위스', 'CH', '2027-04-12T10:00:00', '루체른 구시가지·카펠교', '루체른 카펠교'],
  ['ch-0413', '스위스', 'CH', '2027-04-13T13:30:00', '인터라켄·브리엔츠 호수', '브리엔츠 호수'],
  ['ch-0414', '스위스', 'CH', '2027-04-14T09:30:00', '취리히 공항·리스본 이동', '취리히 공항'],
  ['pt-0414', '포르투갈', 'PT', '2027-04-14T17:00:00', '리스본 호텔·코메르시우 광장', '코메르시우 광장'],
  ['pt-0415', '포르투갈', 'PT', '2027-04-15T10:00:00', '벨렙·제로니무스 수도원', '제로니무스 수도원'],
  ['pt-0416', '포르투갈', 'PT', '2027-04-16T09:30:00', '신트라 페나성', '페나 궁전'],
  ['pt-0417', '포르투갈', 'PT', '2027-04-17T16:00:00', '알파마·28번 트램', '알파마'],
  ['pt-0418', '포르투갈', 'PT', '2027-04-18T11:00:00', '타임아웃 마켓·마지막 산책', '타임아웃 마켓'],
]

function demoScheduleRow(item, index) {
  const [, countryName, countryCode, scheduledAt, scheduleName, placeName] = item
  return {
    id: 97_001 + index,
    tripCountryId: null,
    countryName,
    countryCode,
    timeZone: countryCode === 'PT' ? 'Europe/Lisbon' : countryCode === 'CH' ? 'Europe/Zurich' : 'Europe/Paris',
    scheduleName,
    scheduledAt,
    currencyCode: countryCode === 'CH' ? 'CHF' : 'EUR',
    amount: 0,
    paymentStatus: 'UNDECIDED',
    scheduleStatus: 'UPCOMING',
    placeName,
    placeAddress: '',
    memo: '',
  }
}

export function demoTravelSchedules() {
  return demoSchedules.map(demoScheduleRow)
}

export function demoTravelScheduleDetail(scheduleId) {
  return demoTravelSchedules().find(item => item.id === Number(scheduleId)) || null
}

const demoReceiptTemplates = [
  {
    id: 98_001, countryName: '프랑스', categoryId: 5, categoryName: '취미여가',
    merchantOriginalName: 'Musée du Louvre', merchantTranslatedName: '루브르 박물관',
    paymentDateTime: '2027-04-05T10:18:00', currencyCode: 'EUR', currencyName: '유로', currencySymbol: '€', totalAmount: 90,
    items: [['Billet Musée du Louvre', '루브르 박물관 입장권', 1, 90]], participants: ['유현', '유진'],
  },
  {
    id: 98_002, countryName: '프랑스', categoryId: 1, categoryName: '식비',
    merchantOriginalName: 'Bouillon Chartier', merchantTranslatedName: '부이옹 샤르티에',
    paymentDateTime: '2027-04-06T12:35:00', currencyCode: 'EUR', currencyName: '유로', currencySymbol: '€', totalAmount: 32,
    items: [['Menu du jour', '오늘의 메뉴', 1, 24], ['Café', '커피', 1, 8]], participants: [],
  },
  {
    id: 98_003, countryName: '스위스', categoryId: 9, categoryName: '취미여가',
    merchantOriginalName: 'Jungfraujoch - Top of Europe', merchantTranslatedName: '융프라우요흐 전망대',
    paymentDateTime: '2027-04-10T09:12:00', currencyCode: 'CHF', currencyName: '스위스 프랑', currencySymbol: 'CHF', totalAmount: 70,
    items: [['Jungfraujoch Ticket', '융프라우요흐 입장권', 1, 70]], participants: ['유현', '유진'],
  },
  {
    id: 98_004, countryName: '스위스', categoryId: 1, categoryName: '식비',
    merchantOriginalName: 'Zeughauskeller', merchantTranslatedName: '추어크하우스켈러',
    paymentDateTime: '2027-04-12T18:42:00', currencyCode: 'CHF', currencyName: '스위스 프랑', currencySymbol: 'CHF', totalAmount: 28.2,
    items: [['Zürcher Geschnetzeltes', '취리히식 송아지 요리', 1, 28.2]], participants: [],
  },
  {
    id: 98_005, countryName: '포르투갈', categoryId: 9, categoryName: '취미여가',
    merchantOriginalName: 'Palácio Nacional da Pena', merchantTranslatedName: '페나 궁전',
    paymentDateTime: '2027-04-16T10:08:00', currencyCode: 'EUR', currencyName: '유로', currencySymbol: '€', totalAmount: 105,
    items: [['Palace & Park Ticket', '페나 궁전·공원 패스', 3, 35]], participants: ['유현', '유진'],
  },
  {
    id: 98_006, countryName: '포르투갈', categoryId: 1, categoryName: '식비',
    merchantOriginalName: 'Time Out Market Lisboa', merchantTranslatedName: '타임아웃 마켓 리스본',
    paymentDateTime: '2027-04-17T18:25:00', currencyCode: 'EUR', currencyName: '유로', currencySymbol: '€', totalAmount: 24.5,
    items: [['Bacalhau', '바칼랰 요리', 1, 18], ['Pastel de nata', '에그타르트', 2, 3.25]], participants: [],
  },
]

const demoSettlementState = new Map([['유현', false], ['유진', false]])

function receiptWithContext(template, tripId, countryIds = {}) {
  const splitCount = template.participants.length ? template.participants.length + 1 : 1
  return {
    ...template,
    tripId: Number(tripId),
    countryId: Number(countryIds[template.countryName]) || null,
    currencyId: template.currencyCode === 'CHF' ? 2 : 1,
    splitCount,
    splitAmount: Number((template.totalAmount / splitCount).toFixed(2)),
    participantNames: template.participants.join(', '),
    participants: template.participants.map((participantName, index) => ({
      id: template.id * 10 + index + 1,
      participantName,
      displayOrder: index + 1,
    })),
    items: template.items.map(([originalName, translatedName, quantity, amount], index) => ({
      id: template.id * 10 + index + 1,
      originalName,
      translatedName,
      quantity,
      amount,
      displayOrder: index + 1,
    })),
    fileName: null,
    fileUrl: '',
    fileType: null,
    memo: '',
    status: 'COMPLETED',
    ocrRawText: '',
    createdAt: template.paymentDateTime,
    updatedAt: template.paymentDateTime,
  }
}

export function demoReceipts(tripId, countryIds = {}, params = {}) {
  const cutoff = todayIso()
  return demoReceiptTemplates
    .filter(item => item.paymentDateTime.slice(0, 10) <= cutoff)
    .filter(item => !params.startDate || item.paymentDateTime.slice(0, 10) >= params.startDate)
    .filter(item => !params.endDate || item.paymentDateTime.slice(0, 10) <= params.endDate)
    .map(item => receiptWithContext(item, tripId, countryIds))
}

export function demoReceiptDetail(tripId, receiptId, countryIds = {}) {
  const template = demoReceiptTemplates.find(item => item.id === Number(receiptId))
  return template ? receiptWithContext(template, tripId, countryIds) : null
}

export function demoReceiptDates() {
  const cutoff = todayIso()
  return [...new Set(demoReceiptTemplates
    .map(item => item.paymentDateTime.slice(0, 10))
    .filter(date => date <= cutoff))]
}

function participantReceipts(tripId, participantName, countryIds = {}) {
  return demoReceipts(tripId, countryIds).filter(receipt =>
    receipt.participants.some(participant => participant.participantName === participantName))
}

function aggregateParticipantAmounts(receipts) {
  const amounts = new Map()
  receipts.forEach(receipt => {
    amounts.set(receipt.currencyCode, {
      currencyCode: receipt.currencyCode,
      currencySymbol: receipt.currencySymbol,
      amount: Number(((amounts.get(receipt.currencyCode)?.amount || 0) + receipt.splitAmount).toFixed(2)),
    })
  })
  return [...amounts.values()]
}

export function demoReceiptSettlements(tripId, countryIds = {}) {
  const participants = ['유현', '유진'].map(participantName => {
    const receipts = participantReceipts(tripId, participantName, countryIds)
    return {
      participantName,
      receiptCount: receipts.length,
      settled: demoSettlementState.get(participantName) === true,
      amounts: aggregateParticipantAmounts(receipts),
    }
  }).filter(participant => participant.receiptCount > 0)
  const totalAmounts = new Map()
  participants.forEach(participant => participant.amounts.forEach(amount => {
    const current = totalAmounts.get(amount.currencyCode) || { ...amount, amount: 0 }
    current.amount = Number((current.amount + amount.amount).toFixed(2))
    totalAmounts.set(amount.currencyCode, current)
  }))
  return { totalAmounts: [...totalAmounts.values()], participantCount: participants.length, participants }
}

export function demoParticipantSettlement(tripId, participantName, countryIds = {}) {
  const receipts = participantReceipts(tripId, participantName, countryIds)
  return {
    participantName,
    totalOwedAmount: receipts.reduce((sum, receipt) => sum + receipt.splitAmount, 0),
    receipts,
  }
}

export function toggleDemoReceiptSettlement(participantName, settled) {
  demoSettlementState.set(participantName, Boolean(settled))
}

export function isLay1217Demo() {
  try {
    const user = JSON.parse(localStorage.getItem('tripass-user') || 'null')
    const demoAccounts = new Set(['lay1217', 'ahyoung021217@gmail.com'])
    return [user?.loginId, user?.email, user?.username]
      .map(value => String(value || '').trim().toLowerCase())
      .some(identity => demoAccounts.has(identity))
  } catch {
    return false
  }
}

export function demoStage() {
  const date = todayIso()
  if (date <= '2027-04-04') return 'dday'
  if (date <= '2027-04-12') return 'day9'
  if (date <= '2027-04-18') return 'day13'
  return 'end'
}

function stageData(name, stage = demoStage()) {
  if (stage === 'dday') return { total: 0, categories: Array(7).fill(0) }
  return countryData[name]?.[stage] || { total: 0, categories: Array(7).fill(0) }
}

function normalizeName(value) {
  return value === '취미·여가' ? '취미여가' : value
}

export function overlayTravelStatus(base = {}, requestedCountryId = null) {
  const stage = demoStage()
  const countries = (base.countries || []).map(country => {
    if (country.tripCountryId != null) countryIdNames.set(String(country.tripCountryId), country.countryName)
    const mock = countryData[country.countryName]
    const state = stageData(country.countryName, stage)
    return mock ? { ...country, targetBudget: mock.budget, spentAmount: state.total } : country
  })
  const selected = requestedCountryId
    ? countries.find(country => Number(country.tripCountryId) === Number(requestedCountryId))
    : null
  const names = selected ? [selected.countryName] : Object.keys(countryData)
  const categorySummary = categoryNames.map((categoryName, index) => {
    const countryDetails = names.map(name => ({
      countryName: name,
      amount: stageData(name, stage).categories[index],
    }))
    return {
      categoryName,
      totalAmount: countryDetails.reduce((sum, item) => sum + item.amount, 0),
      countryDetails,
    }
  })
  const requestedCountry = requestedCountryId
    ? countries.find(country => Number(country.tripCountryId) === Number(requestedCountryId))
    : null
  const countryIds = new Map(countries.map(country => [country.countryName, country.tripCountryId]))
  const upcomingSchedules = demoSchedules
    .filter(([, countryName]) => !requestedCountry || countryName === requestedCountry.countryName)
    .map(([scheduleId, countryName, countryCode, dateTime, title, location]) => ({
      scheduleId: `lay-demo-${scheduleId}`,
      tripCountryId: countryIds.get(countryName),
      countryName,
      countryCode,
      title,
      dateTime,
      location,
    }))
  return {
    ...base,
    totalRemainingFund: TARGET - countries.reduce((sum, item) => sum + Number(item.spentAmount || 0), 0),
    countries,
    categorySummary,
    upcomingSchedules,
  }
}

export function overlayBudgetCheck(base = []) {
  const stage = demoStage()
  return (base || []).map(item => {
    if (item.tripCountryId != null) countryIdNames.set(String(item.tripCountryId), item.countryName)
    const mock = countryData[item.countryName]
    if (!mock) return item
    const state = stageData(item.countryName, stage)
    return {
      ...item,
      targetBudget: mock.budget,
      preExpenseTotal: 0,
      travelExpenseTotal: state.total,
      remainingFund: mock.budget - state.total,
      categoryBreakdown: categoryNames.map((categoryName, index) => ({
        categoryName,
        amount: state.categories[index],
      })),
    }
  })
}

const walletSnapshots = {
  dday: { balance: 3_438_008, emergency: 600_008, eur: [308.64, 500_000], chf: [0, 0] },
  day9: { balance: 2_088_008, emergency: 600_008, eur: [33.68, 54_560], chf: [59.78, 106_400] },
  day13: { balance: 400_008, emergency: 400_008, eur: [67.94, 110_060], chf: [37.29, 66_380] },
  end: { balance: 400_008, emergency: 400_008, eur: [0, 0], chf: [37.29, 66_380] },
}

export function demoForeignBalances() {
  const snap = walletSnapshots[demoStage()]
  return [
    { currencyCode: 'EUR', currencyName: '유로', symbol: 'EUR', flag: '🇪🇺', balanceAmount: snap.eur[0], krwEstimatedAmount: snap.eur[1], rate: 1620 },
    { currencyCode: 'CHF', currencyName: '스위스 프랑', symbol: 'CHF', flag: '🇨🇭', balanceAmount: snap.chf[0], krwEstimatedAmount: snap.chf[1], rate: 1780 },
  ]
}

export function overlayWalletMain(base = {}) {
  const snap = walletSnapshots[demoStage()]
  return {
    ...base,
    balanceAmount: snap.balance,
    targetAmount: TARGET,
    emergencyAmount: snap.emergency,
    goalAvailableAmount: Math.min(snap.balance, TARGET),
    externalChargeAmount: 0,
    savingRate: Math.min(100, Math.round((snap.balance / TARGET) * 1000) / 10),
    foreignBalances: demoForeignBalances(),
  }
}

const fundingLedgers = [
  ['2027-04-04T09:00:00', 500_000, INITIAL_WALLET, 3_438_008, 'EUR 트래블카드 충전'],
  ['2027-04-06T09:00:00', 600_000, 3_438_008, 2_838_008, 'EUR 트래블카드 충전'],
  ['2027-04-09T09:00:00', 750_000, 2_838_008, 2_088_008, 'CHF 트래블카드 충전'],
  ['2027-04-13T09:00:00', 400_000, 2_088_008, 1_688_008, 'CHF 트래블카드 충전'],
  ['2027-04-14T09:00:00', 1_088_000, 1_688_008, 600_008, '남은 여행 목표 자금 EUR 충전'],
  ['2027-04-16T12:00:00', 200_000, 600_008, 400_008, '비상금 EUR 충전'],
]

export function demoWalletLedgers(base = []) {
  const cutoff = todayIso()
  const rows = fundingLedgers.filter(item => item[0].slice(0, 10) <= cutoff).map((item, index) => ({
    ledgerId: `lay-demo-${index + 1}`,
    direction: 'OUT', transactionType: 'TRAVEL_CARD_TOPUP', amount: item[1],
    balanceBefore: item[2], balanceAfter: item[3], memo: item[4], createdAt: item[0],
  }))
  return [...rows, ...(base || []).filter(item => String(item.createdAt || '').slice(0, 10) < '2027-04-04')]
}

export function demoPostTripReport(tripId) {
  const categories = [565_560, 957_180, 163_600, 586_780, 110_080, 981_660, 106_760]
  const groupedDaily = new Map()
  daily.forEach(([date, , amount]) => groupedDaily.set(date, (groupedDaily.get(date) || 0) + amount))
  const countrySpending = Object.entries(countryData).map(([countryName, item]) => ({
    countryName, amount: item.end.total, budget: item.budget,
  }))
  return {
    tripId,
    tripName: '유럽 3개국 여행', countryNames: Object.keys(countryData),
    startDate: '2027-04-04', endDate: '2027-04-18', days: 15,
    targetBudget: TARGET, spent: 3_471_620, remaining: -133_620,
    dailyAverage: Math.round(3_471_620 / 15), savingsRate: -4,
    dailySpending: [...groupedDaily].map(([date, amount]) => ({ date, amount })),
    categorySpending: categoryNames.map((categoryName, index) => ({ categoryName, amount: categories[index] })),
    countrySpending,
    countryTopCategories: countrySpending.map(country => {
      const state = countryData[country.countryName].end
      const max = Math.max(...state.categories)
      const index = state.categories.indexOf(max)
      return { countryName: country.countryName, categoryName: categoryNames[index], amount: max, countryTotal: state.total }
    }),
    receiptCount: demoReceiptTemplates.length, nextTripMonthlySuggestion: 460_000, nextTripMonths: 8,
  }
}

export function demoTransactions(base = [], countryId = null, categoryName = '') {
  const normalizedCategory = normalizeName(categoryName)
  const baseCountry = (base || [])[0]?.countryName
  const countryName = baseCountry || (countryId ? countryIdNames.get(String(countryId)) : '')
  const cutoff = todayIso()
  const rows = daily.filter(([date, name]) => date <= cutoff && (!countryName || name === countryName))
  const rowTotals = rows.reduce((result, [, name, amount]) => {
    result[name] = (result[name] || 0) + amount
    return result
  }, {})
  const rowCounts = rows.reduce((result, [, name]) => {
    result[name] = (result[name] || 0) + 1
    return result
  }, {})
  const allocated = {}
  const visited = {}
  return rows.map(([date, name, total], index) => {
    const state = countryData[name]?.[demoStage()]
    const categoryIndex = normalizedCategory ? categoryNames.indexOf(normalizedCategory) : -1
    const categoryTotal = Number(state?.categories?.[categoryIndex] || 0)
    visited[name] = (visited[name] || 0) + 1
    let categoryAmount = total
    if (categoryIndex >= 0) {
      const isLast = visited[name] === rowCounts[name]
      categoryAmount = isLast
        ? categoryTotal - (allocated[name] || 0)
        : Math.round(categoryTotal * total / Math.max(rowTotals[name], 1))
      allocated[name] = (allocated[name] || 0) + categoryAmount
    }
    const displayCategory = normalizedCategory || '기타'
    const merchants = merchantNames[name]?.[displayCategory] || [`${name} 현지 결제`]
    const transaction = {
      transactionId: `lay-demo-${name}-${displayCategory}-${date}-${index}`,
      countryName: name,
      countryCode: name === '스위스' ? 'CH' : name === '포르투갈' ? 'PT' : 'FR',
      categoryId: categoryIds[displayCategory] || 6,
      categoryName: displayCategory,
      merchantName: merchants[index % merchants.length], amount: categoryAmount,
      originalAmount: categoryAmount / (name === '스위스' ? 1780 : 1620),
      appliedExchangeRate: name === '스위스' ? 1780 : 1620,
      currencySymbol: name === '스위스' ? 'CHF' : 'EUR', transactionDate: date,
      transactionTime: `${String(10 + (index % 9)).padStart(2, '0')}:${index % 2 ? '35' : '10'}:00`,
      transactionType: 'WITHDRAWAL', paymentMethodName: 'KB 트래블러스 체크카드',
      memo: `${name} 여행 중 ${displayCategory} 결제`,
    }
    transactionCache.set(String(transaction.transactionId), transaction)
    return transaction
  }).filter(item => item.amount > 0)
}

export function getDemoTransaction(transactionId) {
  return transactionCache.get(String(transactionId)) || null
}
