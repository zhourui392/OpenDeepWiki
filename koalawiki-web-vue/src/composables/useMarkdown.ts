import MarkdownIt from 'markdown-it'
// @ts-ignore
import katex from 'markdown-it-katex'
import anchor from 'markdown-it-anchor'
import hljs from 'highlight.js'

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

  const md = new MarkdownIt({
    html: true,
    linkify: true,
    typographer: true,
    highlight: enableHighlight ? (str, lang) => {
      if (lang && hljs.getLanguage(lang)) {
        try {
          return hljs.highlight(str, { language: lang }).value
        } catch {}
      }
      return ''
    } : undefined
  })

  if (enableKatex) {
    md.use(katex, {
      throwOnError: false,
      errorColor: '#cc0000'
    })
  }

  if (enableAnchor) {
    md.use(anchor, {
      permalink: anchor.permalink.headerLink()
    })
  }

  const render = (content: string): string => {
    return md.render(content)
  }

  const extractToc = (content: string): TocItem[] => {
    const tokens = md.parse(content, {})
    const headings: TocItem[] = []

    for (let i = 0; i < tokens.length; i++) {
      const token = tokens[i]
      if (token && token.type === 'heading_open') {
        const level = parseInt(token.tag.slice(1))
        const textToken = tokens[i + 1]
        const text = textToken?.content || ''
        const id = token.attrGet('id') || ''
        headings.push({ level, text, id })
      }
    }

    return headings
  }

  return {
    render,
    extractToc
  }
}
