declare namespace API {
  interface PageResult<T> {
    records: T[]
    total: number
    page: number
    size: number
  }

  interface ApiResponse<T = any> {
    code: number
    message: string
    data: T
  }
}
