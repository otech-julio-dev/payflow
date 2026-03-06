import client from './client'

export const walletApi = {
  getAccount: ()       => client.get('/api/wallets/me'),
  getBalance: ()       => client.get('/api/wallets/me/balance'),
  topUp:      (data)   => client.post('/api/wallets/topup', data),
}