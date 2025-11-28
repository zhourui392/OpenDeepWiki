/**
 * Pinia Store 统一导出
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
import { createPinia } from 'pinia'

export const pinia = createPinia()

export { useClusterStore } from './cluster'
export { useSearchStore } from './search'
