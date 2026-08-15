import type { AxiosResponse } from 'axios'

/**
 * 后端响应契约类型。request.ts 的 ApiResult 与分页 TableData 的对外形态。
 */

export interface ApiResult<T = unknown> {
  code: number
  msg: string
  data: T
}

/** 分页响应(table 端点直接平铺 total/rows,非 AjaxResult 包装)。 */
export interface TableData<T> {
  code: number
  msg: string
  total: number
  rows: T[]
}

export type { AxiosResponse }
