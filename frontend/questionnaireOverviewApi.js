import axios from 'axios'

const BASE_URL = '/api/product-questionnaires/data-overview'

export async function downloadQuestionnaireImportTemplate() {
  const response = await axios.get(`${BASE_URL}/import-template`, {
    responseType: 'blob'
  })

  const disposition = response.headers['content-disposition'] || ''
  const matched = disposition.match(/filename\*=UTF-8''([^;]+)/i)
  const fileName = matched
    ? decodeURIComponent(matched[1])
    : '问卷观点导入模板.xlsx'

  const url = URL.createObjectURL(response.data)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = fileName
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  URL.revokeObjectURL(url)
}

export async function importQuestionnaireExcel(file) {
  const formData = new FormData()
  formData.append('file', file)
  const response = await axios.post(`${BASE_URL}/import`, formData)
  return response.data
}
