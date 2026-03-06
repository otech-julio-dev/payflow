import client from './client'

export const transactionApi = {
  getAll: (page = 0, size = 20) =>
    client.get(`/api/transactions/my?page=${page}&size=${size}`),
  getById: (id) => client.get(`/api/transactions/${id}`),
}
