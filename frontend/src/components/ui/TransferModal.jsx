import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { transferApi } from '../../api/transfer'
import toast from 'react-hot-toast'
import { X, ArrowUpRight, Send } from 'lucide-react'
import Spinner from './Spinner'

export default function TransferModal({ onClose }) {
  const queryClient = useQueryClient()
  const [form, setForm] = useState({
    targetAccountNumber: '',
    amount: '',
    description: '',
  })

  const mutation = useMutation({
    mutationFn: (data) => transferApi.send(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['account'] })
      queryClient.invalidateQueries({ queryKey: ['transactions'] })
      toast.success('¡Transferencia enviada exitosamente!')
      onClose()
    },
    onError: (err) => {
      toast.error(err.response?.data?.message || 'Error al realizar transferencia')
    }
  })

  const handleSubmit = (e) => {
    e.preventDefault()
    if (!form.targetAccountNumber.trim()) {
      toast.error('Ingresa el número de cuenta destino')
      return
    }
    if (!form.amount || parseFloat(form.amount) <= 0) {
      toast.error('Ingresa un monto válido')
      return
    }
    mutation.mutate({
      targetAccountNumber: form.targetAccountNumber.trim(),
      amount: parseFloat(form.amount),
      description: form.description.trim() || null,
    })
  }

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 px-4">
      <div className="bg-white rounded-3xl w-full max-w-md shadow-2xl">

        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-gray-100">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 bg-blue-50 rounded-xl flex items-center justify-center">
              <ArrowUpRight className="w-4 h-4 text-blue-600" />
            </div>
            <h2 className="font-bold text-gray-900">Transferir dinero</h2>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-xl hover:bg-gray-100 transition-colors"
          >
            <X className="w-5 h-5 text-gray-400" />
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4">

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1.5">
              Cuenta destino
            </label>
            <input
              type="text"
              placeholder="PF-0000000000-XXXX"
              value={form.targetAccountNumber}
              onChange={(e) => setForm(f => ({
                ...f, targetAccountNumber: e.target.value
              }))}
              className="input-field font-mono"
              autoFocus
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1.5">
              Monto
            </label>
            <div className="relative">
              <span className="absolute left-3 top-1/2 -translate-y-1/2
                               text-gray-400 font-medium text-sm">$</span>
              <input
                type="number"
                step="0.01"
                min="0.01"
                placeholder="0.00"
                value={form.amount}
                onChange={(e) => setForm(f => ({ ...f, amount: e.target.value }))}
                className="input-field pl-7"
              />
              <span className="absolute right-3 top-1/2 -translate-y-1/2
                               text-gray-400 text-xs">MXN</span>
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1.5">
              Descripción <span className="text-gray-400 font-normal">(opcional)</span>
            </label>
            <input
              type="text"
              placeholder="Ej. Pago de renta"
              value={form.description}
              onChange={(e) => setForm(f => ({ ...f, description: e.target.value }))}
              className="input-field"
              maxLength={100}
            />
          </div>

          <div className="flex gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="btn-secondary flex-1"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={mutation.isPending}
              className="btn-primary flex-1 flex items-center justify-center gap-2"
            >
              {mutation.isPending
                ? <Spinner size="sm" />
                : <>
                    <Send className="w-4 h-4" />
                    Enviar
                  </>
              }
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
