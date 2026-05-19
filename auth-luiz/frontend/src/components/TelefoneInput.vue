<template>
  <div class="input-group">
    <select
      v-model="paisSelecionado"
      class="form-select"
      style="width: auto; flex: 0 0 auto;"
      :disabled="disabled"
      @change="aoTrocarPais"
    >
      <option v-for="p in paises" :key="p.code" :value="p">{{ p.flag }} +{{ p.ddi }}</option>
    </select>
    <input
      ref="inputRef"
      type="tel"
      inputmode="numeric"
      class="form-control"
      :value="displayValue"
      :placeholder="paisSelecionado.placeholder"
      :required="required"
      :disabled="disabled"
      @input="aoDigitar"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  required:   { type: Boolean, default: false },
  disabled:   { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue'])

const paises = [
  { code: 'BR', flag: '🇧🇷', ddi: '55',  minDigitos: 10, maxDigitos: 11, placeholder: '(11) 99999-9999' },
  { code: 'US', flag: '🇺🇸', ddi: '1',   minDigitos: 10, maxDigitos: 10, placeholder: '(555) 555-5555'  },
  { code: 'AR', flag: '🇦🇷', ddi: '54',  minDigitos: 10, maxDigitos: 10, placeholder: '(11) 9876-5432'  },
  { code: 'PT', flag: '🇵🇹', ddi: '351', minDigitos: 9,  maxDigitos: 9,  placeholder: '912 345 678'     },
  { code: 'ES', flag: '🇪🇸', ddi: '34',  minDigitos: 9,  maxDigitos: 9,  placeholder: '612 345 678'     },
]

const paisSelecionado = ref(paises[0])
const digitosLocais = ref('')
const inputRef = ref(null)

function aplicarMascara(digits, pais) {
  const d = digits.slice(0, pais.maxDigitos)
  switch (pais.code) {
    case 'BR':
      if (d.length <= 2)  return d
      if (d.length <= 6)  return `(${d.slice(0, 2)}) ${d.slice(2)}`
      if (d.length <= 10) return `(${d.slice(0, 2)}) ${d.slice(2, 6)}-${d.slice(6)}`
      return `(${d.slice(0, 2)}) ${d.slice(2, 7)}-${d.slice(7)}`
    case 'US':
      if (d.length <= 3) return d
      if (d.length <= 6) return `(${d.slice(0, 3)}) ${d.slice(3)}`
      return `(${d.slice(0, 3)}) ${d.slice(3, 6)}-${d.slice(6)}`
    case 'AR':
      if (d.length <= 2) return d
      if (d.length <= 6) return `(${d.slice(0, 2)}) ${d.slice(2)}`
      return `(${d.slice(0, 2)}) ${d.slice(2, 6)}-${d.slice(6)}`
    default:
      // Portugal, Espanha e outros: grupos de 3 separados por espaço
      return d.replace(/(\d{3})(?=\d)/g, '$1 ')
  }
}

const displayValue = computed(() => aplicarMascara(digitosLocais.value, paisSelecionado.value))

function aoDigitar(event) {
  const raw = event.target.value.replace(/\D/g, '').slice(0, paisSelecionado.value.maxDigitos)
  digitosLocais.value = raw
  // Força re-render do valor mascarado sem depender de v-model
  event.target.value = aplicarMascara(raw, paisSelecionado.value)
  emitirValor()
}

function aoTrocarPais() {
  digitosLocais.value = ''
  emitirValor()
  inputRef.value?.focus()
}

function emitirValor() {
  emit('update:modelValue', digitosLocais.value
    ? `+${paisSelecionado.value.ddi}${digitosLocais.value}`
    : '')
}

function parsearModelValue(val) {
  if (!val) { digitosLocais.value = ''; return }
  // Tenta encontrar o país pelo DDI (ordena desc por comprimento para evitar ambiguidade: 351 antes de 35)
  const sorted = [...paises].sort((a, b) => b.ddi.length - a.ddi.length)
  for (const p of sorted) {
    if (val.startsWith(`+${p.ddi}`)) {
      paisSelecionado.value = paises.find(x => x.code === p.code) ?? paises[0]
      digitosLocais.value = val.slice(1 + p.ddi.length).replace(/\D/g, '').slice(0, p.maxDigitos)
      return
    }
  }
  // Fallback: Brasil
  paisSelecionado.value = paises[0]
  digitosLocais.value = val.replace(/\D/g, '').slice(0, paises[0].maxDigitos)
}

onMounted(() => parsearModelValue(props.modelValue))

watch(() => props.modelValue, (val) => {
  const atual = digitosLocais.value ? `+${paisSelecionado.value.ddi}${digitosLocais.value}` : ''
  if (val !== atual) parsearModelValue(val)
})
</script>
