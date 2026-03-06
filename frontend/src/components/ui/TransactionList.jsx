import { useQuery } from '@tanstack/react-query'
import { transactionApi } from '../../api/transaction'
import { ArrowUpRight, ArrowDownLeft, Plus, RefreshCw } from 'lucide-react'
import Spinner from './Spinner'

function TransactionItem({ tx }) {
  const isCredit = tx.type === 'CREDIT' || tx.type === 'TOPUP'
  const isTopup  = tx.type === 'TOPUP'

  const icon = isTopup
    ? <Plus className="w-4 h-4 text-green-600" />
    : isCredit
      ? <ArrowDownLeft className="w-4 h-4 text-green-600" />
      : <ArrowUpRight className="w-4 h-4 text-red-500" />

  const iconBg = isCredit ? 'bg-green-50' : 'bg-red-50'

  const label = isTopup
    ? 'Depósito'
    : isCredit
      ? `De ${tx.counterpartyAccountNumber || '—'}`
      : `A ${tx.counterpartyAccountNumber || '—'}`

  const amountColor = isCredit ? 'text-green-600' : 'text-red-500'
  const amountSign  = isCredit ? '+' : '-'

  const date = new Date(tx.createdAt).toLocaleDateString('es-MX', {
    day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit'
  })

  return (
    <div className="flex items-center gap-3 py-3
                    border-b border-gray-50 last:border-0">
      <div className={`w-9 h-9 rounded-xl flex items-center
                       justify-center flex-shrink-0 ${iconBg}`}>
        {icon}
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-sm font-medium text-gray-900 truncate">
          {tx.description || label}
        </p>
        <p className="text-xs text-gray-400 mt-0.5 font-mono truncate">
          {label} · {date}
        </p>
      </div>
      <div className="text-right flex-shrink-0">
        <p className={`text-sm font-semibold ${amountColor}`}>
          {amountSign}${Number(tx.amount).toLocaleString('es-MX', {
            minimumFractionDigits: 2
          })}
        </p>
        <p className="text-xs text-gray-400 mt-0.5">
          Saldo: ${Number(tx.balanceAfter).toLocaleString('es-MX', {
            minimumFractionDigits: 2
          })}
        </p>
      </div>
    </div>
  )
}

export default function TransactionList() {
  const { data, isLoading, refetch, isFetching } = useQuery({
    queryKey: ['transactions'],
    queryFn:  () => transactionApi.getAll(0, 20).then(r => r.data),
    staleTime: 10_000,
  })

  const transactions = data?.content ?? []

  return (
    <div className="card">
      <div className="flex items-center justify-between mb-4">
        <h3 className="font-semibold text-gray-900 text-sm">
          Historial de movimientos
        </h3>
        <button
          onClick={() => refetch()}
          className="p-1.5 rounded-lg hover:bg-gray-100 transition-colors"
          title="Actualizar"
        >
          <RefreshCw className={`w-4 h-4 text-gray-400
            ${isFetching ? 'animate-spin' : ''}`} />
        </button>
      </div>

      {isLoading ? (
        <div className="flex justify-center py-8">
          <Spinner size="md" />
        </div>
      ) : transactions.length === 0 ? (
        <div className="text-center py-8">
          <div className="w-12 h-12 bg-gray-50 rounded-2xl flex items-center
                          justify-center mx-auto mb-3">
            <ArrowUpRight className="w-6 h-6 text-gray-300" />
          </div>
          <p className="text-sm text-gray-400 font-medium">Sin movimientos aún</p>
          <p className="text-xs text-gray-300 mt-1">
            Tus transacciones aparecerán aquí
          </p>
        </div>
      ) : (
        <>
          <div>
            {transactions.map(tx => (
              <TransactionItem key={tx.id} tx={tx} />
            ))}
          </div>
          {data?.totalElements > 20 && (
            <p className="text-xs text-gray-400 text-center mt-3">
              Mostrando 20 de {data.totalElements} movimientos
            </p>
          )}
        </>
      )}
    </div>
  )
}
