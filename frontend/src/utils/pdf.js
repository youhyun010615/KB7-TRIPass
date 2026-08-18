/**
 * 지정한 DOM 요소를 PDF 파일로 저장한다.
 * html2pdf.js는 번들 크기가 커서 실제 저장 시점에 동적 import한다.
 */
export async function exportElementToPdf(element, filename) {
  if (!element) return

  const { default: html2pdf } = await import('html2pdf.js')

  await html2pdf()
    .set({
      filename,
      margin: 0,
      image: { type: 'jpeg', quality: 0.98 },
      html2canvas: { scale: 2, backgroundColor: '#f8f6f1', useCORS: true },
      jsPDF: { unit: 'pt', format: 'a4', orientation: 'portrait' },
      pagebreak: { mode: ['avoid-all', 'css', 'legacy'] },
    })
    .from(element)
    .save()
}
