import { unified } from 'unified'
import remarkParse from 'remark-parse'
import remarkGfm from 'remark-gfm'
import remarkMath from 'remark-math'
import remarkRehype from 'remark-rehype'
import rehypeHighlight from 'rehype-highlight'
import rehypeSlug from 'rehype-slug'
import rehypeAutolinkHeadings from 'rehype-autolink-headings'
import rehypeKatex from 'rehype-katex'
import rehypeStringify from 'rehype-stringify'
import { visit } from 'unist-util-visit'

interface MarkdownOptions {
  enableKatex?: boolean
  enableAnchor?: boolean
  enableHighlight?: boolean
}

interface TocItem {
  level: number
  text: string
  id: string
}

export function useMarkdown(options: MarkdownOptions = {}) {
  const {
    enableKatex = true,
    enableAnchor = true,
    enableHighlight = true
  } = options

  const createProcessor = () => {
    let processor: any = unified()
      .use(remarkParse)
      .use(remarkGfm)

    if (enableKatex) {
      processor = processor.use(remarkMath)
    }

    processor = processor.use(remarkRehype, { allowDangerousHtml: true })

    if (enableHighlight) {
      processor = processor.use(rehypeHighlight)
    }

    processor = processor.use(rehypeSlug)

    if (enableAnchor) {
      processor = processor.use(rehypeAutolinkHeadings, {
        behavior: 'wrap'
      })
    }

    if (enableKatex) {
      processor = processor.use(rehypeKatex, {
        throwOnError: false,
        errorColor: '#cc0000'
      })
    }

    processor = processor.use(rehypeStringify, { allowDangerousHtml: true })

    return processor
  }

  const render = (content: string): string => {
    try {
      const processor = createProcessor()
      const result = processor.processSync(content)
      return String(result)
    } catch (error) {
      console.error('Markdown render error:', error)
      return '<p>渲染失败</p>'
    }
  }

  const extractToc = (content: string): TocItem[] => {
    const headings: TocItem[] = []

    try {
      const processor: any = unified()
        .use(remarkParse)
        .use(remarkGfm)

      const tree = processor.parse(content)
      const ast = processor.runSync(tree)

      visit(ast, 'heading', (node: any) => {
        const level = node.depth
        const text = node.children
          .filter((child: any) => child.type === 'text' || child.type === 'inlineCode')
          .map((child: any) => child.value)
          .join('')

        // 生成 ID（与 rehype-slug 逻辑一致）
        const id = text
          .toLowerCase()
          .replace(/[^\w\s\u4e00-\u9fa5-]/g, '') // 支持中文
          .replace(/\s+/g, '-')
          .replace(/-+/g, '-')
          .trim()

        if (text && id) {
          headings.push({ level, text, id })
        }
      })
    } catch (error) {
      console.error('TOC extraction error:', error)
    }

    return headings
  }

  return {
    render,
    extractToc
  }
}
