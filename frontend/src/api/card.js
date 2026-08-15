import api from './index'

export function getCardInstitutions() {
  return api.get('/cards/institutions')
}

export function getCards() {
  return api.get('/cards')
}

export function deleteCard(cardId) {
  return api.delete(`/cards/${cardId}`)
}

export function linkCard(data) {
  return api.post('/cards/codef/connect', data)
}

export function fetchCardTransactions(cardId, startDate, endDate) {
  return api.post(`/cards/${cardId}/transactions/fetch`, null, {
    params: { startDate, endDate },
  })
}

export function getCardTransactions(cardId, startDate, endDate) {
  return api.get(`/cards/${cardId}/transactions`, {
    params: { startDate, endDate },
  })
}
