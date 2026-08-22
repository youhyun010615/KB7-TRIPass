import api from '@/api'

const dataOf = response => response.data?.data

export async function fetchWalletMain() {
  return dataOf(await api.get('/wallet'))
}

export async function fetchWalletLedgers() {
  return dataOf(await api.get('/wallet/ledgers'))
}

export async function fetchWalletMonthlySavingDetail(month) {
  return dataOf(await api.get(`/wallet/monthly-savings/${month}`))
}

export async function fetchWalletForeignBalances() {
  return dataOf(await api.get('/wallet/foreign-balances'))
}

export async function chargeWallet(payload) {
  return dataOf(await api.post('/wallet/charge', payload))
}

export async function withdrawWallet(payload) {
  return dataOf(await api.post('/wallet/withdraw', payload))
}

export async function fetchWalletWithdrawOptions() {
  return dataOf(await api.get('/wallet/withdraw/options'))
}

export async function fetchWalletAccounts() {
  return dataOf(await api.get('/wallet/accounts'))
}

export async function fetchWalletAccountOptions() {
  return dataOf(await api.get('/wallet/accounts/options'))
}

export async function linkWalletAccount(payload) {
  return dataOf(await api.post('/wallet/accounts', payload))
}

export async function unlinkWalletAccount(accountId) {
  return dataOf(await api.delete(`/wallet/accounts/${accountId}`))
}

export async function setWalletPrimaryAccount(accountId) {
  return dataOf(await api.put(`/wallet/accounts/${accountId}/primary`))
}

export async function fetchWalletAutoSaving() {
  return dataOf(await api.get('/wallet/auto-saving'))
}

export async function saveWalletAutoSaving(payload) {
  return dataOf(await api.put('/wallet/auto-saving', payload))
}

export async function deleteWalletAutoSaving() {
  return dataOf(await api.delete('/wallet/auto-saving'))
}

export async function fetchWalletAutoSavingLogs() {
  return dataOf(await api.get('/wallet/auto-saving/logs'))
}

export async function fetchWalletCurrencies() {
  return dataOf(await api.get('/wallet/fx/currencies'))
}

export async function estimateWalletExchange(params) {
  return dataOf(await api.get('/wallet/fx/estimate', { params }))
}

export async function sellWalletExchange(payload) {
  return dataOf(await api.post('/wallet/fx/sell', payload))
}

export async function fetchUserTravelCardOptions() {
  return dataOf(await api.get('/wallet/travel-card/options'))
}

export async function fetchLinkedTravelCard() {
  return dataOf(await api.get('/wallet/travel-card'))
}

export async function linkWalletTravelCard(payload) {
  return dataOf(await api.post('/wallet/travel-card', payload))
}

export async function unlinkWalletTravelCard() {
  return dataOf(await api.delete('/wallet/travel-card'))
}

export async function topupWalletTravelCard(payload) {
  return dataOf(await api.post('/wallet/travel-card/topup', payload))
}

export async function fetchWalletTravelCardBalances() {
  return dataOf(await api.get('/wallet/travel-card/balances'))
}

export async function fetchWalletTravelCardLedgers() {
  return dataOf(await api.get('/wallet/travel-card/ledgers'))
}

export async function fetchWalletTravelCardTransactions() {
  return dataOf(await api.get('/wallet/travel-card/transactions'))
}
