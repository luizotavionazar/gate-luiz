<template>
  <div class="d-flex justify-content-center align-items-center" :class="pequeno ? 'gap-1' : 'gap-2'">
    <template v-for="(_, i) in length" :key="i">
      <span v-if="separadorApos !== null && i === separadorApos" class="fw-bold text-secondary fs-4 user-select-none px-1">–</span>
      <input
        :ref="el => { if (el) inputRefs[i] = el }"
        v-model="chars[i]"
        type="text"
        :inputmode="numerico ? 'numeric' : 'text'"
        maxlength="1"
        class="form-control text-center fw-bold p-1"
        :class="pequeno ? 'fs-5' : 'fs-4'"
        :style="pequeno ? 'width: 34px; height: 44px;' : 'width: 48px; height: 56px;'"
        autocomplete="off"
        @input="aoDigitar(i, $event)"
        @keydown="aoApertarTecla(i, $event)"
        @paste.prevent="aoColar($event)"
      />
    </template>
  </div>
</template>

<script setup>
import { nextTick, onMounted, reactive, ref, watch } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  length: { type: Number, default: 6 },
  numerico: { type: Boolean, default: true },
  separadorApos: { type: Number, default: null },
  pequeno: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'submit'])

const chars = reactive(Array(props.length).fill(''))
const inputRefs = ref([])

function montar() {
  const base = chars.join('')
  if (props.separadorApos !== null && chars.some(c => c !== '')) {
    return `${base.slice(0, props.separadorApos)}-${base.slice(props.separadorApos)}`
  }
  return base
}

function aoDigitar(i, event) {
  let valor = event.target.value
  if (props.numerico) valor = valor.replace(/\D/g, '')
  else valor = valor.toUpperCase().replace(/[^A-Z0-9]/g, '')
  chars[i] = valor.slice(-1)
  emit('update:modelValue', montar())
  if (chars[i] && i < props.length - 1) inputRefs.value[i + 1]?.focus()
}

function aoApertarTecla(i, event) {
  if (event.key === 'Backspace') {
    if (chars[i]) {
      chars[i] = ''
      emit('update:modelValue', montar())
    } else if (i > 0) {
      inputRefs.value[i - 1]?.focus()
    }
  } else if (event.key === 'Enter') {
    emit('submit')
  }
}

function aoColar(event) {
  let texto = (event.clipboardData || window.clipboardData).getData('text')
  texto = texto.replace(/-/g, '')
  if (props.numerico) texto = texto.replace(/\D/g, '')
  else texto = texto.toUpperCase().replace(/[^A-Z0-9]/g, '')
  if (!texto) return
  for (let i = 0; i < props.length; i++) chars[i] = texto[i] || ''
  emit('update:modelValue', montar())
  inputRefs.value[Math.min(texto.length - 1, props.length - 1)]?.focus()
}

watch(() => props.modelValue, (val) => {
  if (!val) {
    for (let i = 0; i < props.length; i++) chars[i] = ''
    nextTick(() => inputRefs.value[0]?.focus())
  }
})

onMounted(() => nextTick(() => inputRefs.value[0]?.focus()))
</script>
