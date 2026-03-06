import client from './client'

export const transferApi = {
  send:    (data) => client.post('/api/transfers', data),
  getAll:  ()     => client.get('/api/transfers/my'),
  getById: (id)   => client.get(`/api/transfers/${id}`),
}
