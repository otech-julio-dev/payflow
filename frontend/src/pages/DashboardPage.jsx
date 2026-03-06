import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useAuth } from '../context/AuthContext'
import { walletApi } from '../api/wallet'
import toast from 'react-hot-toast'
import Spinner from '../components/ui/Spinner'
import TransferModal from '../components/ui/TransferModal'
import TransactionList from '../components/ui/TransactionList'
import {
  CreditCard, TrendingUp, ArrowUpRight, ArrowDownLeft,
  LogOut, RefreshCw, Plus, Copy, CheckCircle
} from 'lucide-react'
import { useState } from 'react'

export default function DashboardPage() {
  const { user, logout }  = useAuth()
  const queryClient       = useQueryClient()
  const [copied, setCopied]         = useState(false)
  const [topUpAmount, setTopUpAmount] = useState('')
  const [showTopUp, setShowTopUp]     = useState(false)
  const [showTransfer, setShowTransfer] = useState(false)

  // ── Queries ───────────────────────────────────────────────
  const { data: account, isLoading: loadingAccount } = useQuery({
    queryKey: ['account'],
    queryFn:  () => walletApi.getAccount().then(r => r.data),
  })

  // ── Mutations ─────────────────────────────────────────────
  const topUpMutation = useMutation({
    mutationFn: (amount) => walletApi.topUp({ amount: parseFloat(amount) }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['account'] })
      queryClient.invalidateQueries({ queryKey: ['transactions'] })
      toast.success('¡Saldo agregado exitosamente!')
      setTopUpAmount('')
      setShowTopUp(false)
    },
    onError: (err) => {
      toast.error(err.response?.data?.message || 'Error al agregar saldo')
    }
  })

  const handleCopy = () => {
    if (account?.accountNumber) {
      navigator.clipboard.writeText(account.accountNumber)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    }
  }

  const handleTopUp = (e) => {
    e.preventDefault()
    if (!topUpAmount || parseFloat(topUpAmount) <= 0) {
      toast.error('Ingresa un monto válido')
      return
    }
    topUpMutation.mutate(topUpAmount)
  }

  if (loadingAccount) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <Spinner size="lg" />
      </div>
    )
  }

  const balance = account?.balance ?? 0

  return (
    <div className="min-h-screen bg-gray-50">

      {/* Transfer Modal */}
      {showTransfer && (
        <TransferModal onClose={() => setShowTransfer(false)} />
      )}

      {/* Header */}
      <header className="bg-white border-b border-gray-100 sticky top-0 z-10">
        <div className="max-w-2xl mx-auto px-4 py-4 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <CreditCard className="w-6 h-6 text-brand-500" />
            <span className="font-bold text-brand-700 text-lg">PayFlow</span>
          </div>
          <div className="flex items-center gap-3">
            <div className="text-right hidden sm:block">
              <p className="text-sm font-medium text-gray-900">{user?.fullName}</p>
              <p className="text-xs text-gray-400">{user?.email}</p>
            </div>
            <button
              onClick={logout}
              className="p-2 rounded-lg hover:bg-gray-100 text-gray-500
                         hover:text-gray-700 transition-colors"
              title="Cerrar sesión"
            >
              <LogOut className="w-5 h-5" />
            </button>
          </div>
        </div>
      </header>

      <main className="max-w-2xl mx-auto px-4 py-6 space-y-4">

        {/* Balance Card */}
        <div className="bg-gradient-to-br from-brand-700 to-brand-500
                        rounded-3xl p-6 text-white shadow-lg">
          <div className="flex items-start justify-between mb-6">
            <div>
              <p className="text-blue-200 text-sm font-medium">Saldo disponible</p>
              <p className="text-4xl font-bold mt-1">
                ${Number(balance).toLocaleString('es-MX', {
                  minimumFractionDigits: 2,
                  maximumFractionDigits: 2
                })}
              </p>
              <p className="text-blue-200 text-xs mt-1">MXN</p>
            </div>
            <button
              onClick={() => {
                queryClient.invalidateQueries({ queryKey: ['account'] })
                queryClient.invalidateQueries({ queryKey: ['transactions'] })
              }}
              className="p-2 rounded-xl bg-white/10 hover:bg-white/20 transition-colors"
            >
              <RefreshCw className="w-5 h-5" />
            </button>
          </div>

          {/* Account Number */}
          <div className="bg-white/10 rounded-2xl p-3 flex items-center justify-between">
            <div>
              <p className="text-blue-200 text-xs">Número de cuenta</p>
              <p className="font-mono text-sm font-semibold mt-0.5">
                {account?.accountNumber ?? '—'}
              </p>
            </div>
            <button
              onClick={handleCopy}
              className="p-2 rounded-lg hover:bg-white/10 transition-colors"
            >
              {copied
                ? <CheckCircle className="w-4 h-4 text-green-300" />
                : <Copy className="w-4 h-4 text-blue-200" />
              }
            </button>
          </div>
        </div>

        {/* Quick Actions */}
        <div className="grid grid-cols-2 gap-3">
          <button
            onClick={() => setShowTopUp(true)}
            className="card flex items-center gap-3 hover:border-brand-500
                       hover:shadow-md transition-all duration-200 cursor-pointer"
          >
            <div className="w-10 h-10 rounded-xl bg-green-50 flex items-center justify-center">
              <ArrowDownLeft className="w-5 h-5 text-green-600" />
            </div>
            <div className="text-left">
              <p className="font-semibold text-gray-900 text-sm">Depositar</p>
              <p className="text-xs text-gray-400">Agregar saldo</p>
            </div>
          </button>

          <button
            onClick={() => setShowTransfer(true)}
            className="card flex items-center gap-3 hover:border-brand-500
                       hover:shadow-md transition-all duration-200 cursor-pointer"
          >
            <div className="w-10 h-10 rounded-xl bg-blue-50 flex items-center justify-center">
              <ArrowUpRight className="w-5 h-5 text-blue-600" />
            </div>
            <div className="text-left">
              <p className="font-semibold text-gray-900 text-sm">Transferir</p>
              <p className="text-xs text-gray-400">Enviar dinero</p>
            </div>
          </button>
        </div>

        {/* Top-up Form */}
        {showTopUp && (
          <div className="card border-green-100">
            <h3 className="font-semibold text-gray-900 mb-4 flex items-center gap-2">
              <Plus className="w-4 h-4 text-green-600" />
              Agregar saldo
            </h3>
            <form onSubmit={handleTopUp} className="flex gap-3">
              <div className="flex-1 relative">
                <span className="absolute left-3 top-1/2 -translate-y-1/2
                                 text-gray-400 font-medium text-sm">$</span>
                <input
                  type="number"
                  step="0.01"
                  min="0.01"
                  placeholder="0.00"
                  value={topUpAmount}
                  onChange={(e) => setTopUpAmount(e.target.value)}
                  className="input-field pl-7"
                  autoFocus
                />
              </div>
              <button
                type="submit"
                disabled={topUpMutation.isPending}
                className="btn-primary flex items-center gap-2 whitespace-nowrap"
              >
                {topUpMutation.isPending ? <Spinner size="sm" /> : 'Agregar'}
              </button>
              <button
                type="button"
                onClick={() => setShowTopUp(false)}
                className="btn-secondary"
              >
                Cancelar
              </button>
            </form>
          </div>
        )}

        {/* Account Status */}
        <div className="card">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-brand-50
                              flex items-center justify-center">
                <TrendingUp className="w-5 h-5 text-brand-500" />
              </div>
              <div>
                <p className="font-semibold text-gray-900 text-sm">Estado de cuenta</p>
                <p className="text-xs text-gray-400">
                  Desde {account?.createdAt
                    ? new Date(account.createdAt).toLocaleDateString('es-MX',
                        { year: 'numeric', month: 'long', day: 'numeric' })
                    : '—'}
                </p>
              </div>
            </div>
            <span className={`px-3 py-1 rounded-full text-xs font-semibold
              ${account?.status === 'ACTIVE'
                ? 'bg-green-50 text-green-700'
                : 'bg-red-50 text-red-700'
              }`}>
              {account?.status === 'ACTIVE' ? '● Activa' : account?.status}
            </span>
          </div>
        </div>

        {/* Transaction History */}
        <TransactionList />

      </main>
    </div>
  )
}
