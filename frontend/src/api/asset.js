import api from './index'

export function getAccounts() {
  return api.get('/accounts')
}

export function getAccountTransactions(accountId, params) {
  return api.get(`/accounts/${accountId}/transactions`, { params })
}

export function linkBank(data) {
  return api.post('/accounts/codef/connect', data)
}

export function fetchTransactions(data) {
  return api.post('/accounts/transactions', data)
}
