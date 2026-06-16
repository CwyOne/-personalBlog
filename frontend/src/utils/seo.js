/**
 * SEO Head Manager
 * Updates document title, meta tags, Open Graph, and Twitter Card tags.
 * Zero dependencies — pure DOM manipulation.
 */

const DEFAULT_SITE_NAME = '子墨的博客'
const DEFAULT_DESC = '一个记录技术、思考与生活的个人博客，分享全栈开发经验与实践。'

// Dynamic site name — updated when admin changes nickname
let siteName = DEFAULT_SITE_NAME

export function setSiteName(name) {
  siteName = name ? `${name}的博客` : DEFAULT_SITE_NAME
}

export function getSiteName() {
  return siteName
}

function setMeta(name, content) {
  if (!content) return
  let el = document.querySelector(`meta[name="${name}"]`)
  if (!el) {
    el = document.createElement('meta')
    el.setAttribute('name', name)
    document.head.appendChild(el)
  }
  el.setAttribute('content', content)
}

function setProperty(property, content) {
  if (!content) return
  let el = document.querySelector(`meta[property="${property}"]`)
  if (!el) {
    el = document.createElement('meta')
    el.setAttribute('property', property)
    document.head.appendChild(el)
  }
  el.setAttribute('content', content)
}

function setCanonical(url) {
  if (!url) return
  let el = document.querySelector('link[rel="canonical"]')
  if (!el) {
    el = document.createElement('link')
    el.setAttribute('rel', 'canonical')
    document.head.appendChild(el)
  }
  el.setAttribute('href', url)
}

/**
 * Update page SEO metadata.
 * @param {Object} opts
 * @param {string} opts.title       - Page title (without site name suffix)
 * @param {string} [opts.description] - Page description
 * @param {string} [opts.image]      - OG image URL
 * @param {string} [opts.url]        - Canonical URL
 * @param {string} [opts.type]       - OG type (website, article, etc.)
 * @param {string} [opts.author]     - Article author
 * @param {string} [opts.publishedTime] - Article publish time (ISO)
 * @param {string[]} [opts.keywords] - Extra keywords
 */
export function updateSEO(opts = {}) {
  const {
    title,
    description = DEFAULT_DESC,
    image,
    url,
    type = 'website',
    author,
    publishedTime,
    keywords,
    noIndex = false
  } = opts

  // Title: "Page Title — Site Name" or just site name
  const fullTitle = title ? `${title} — ${siteName}` : siteName
  document.title = fullTitle

  // Robots
  if (noIndex) {
    setMeta('robots', 'noindex, nofollow')
  } else {
    const el = document.querySelector('meta[name="robots"]')
    if (el) el.remove()
  }

  // Standard meta
  setMeta('description', description)
  if (author) setMeta('author', author)
  if (keywords) setMeta('keywords', keywords)

  // Open Graph
  setProperty('og:title', fullTitle)
  setProperty('og:description', description)
  setProperty('og:type', type)
  if (image) setProperty('og:image', image)
  if (url) setProperty('og:url', url)
  setProperty('og:site_name', siteName)

  // Twitter Card
  setMeta('twitter:title', fullTitle)
  setMeta('twitter:description', description)
  if (image) setMeta('twitter:image', image)

  // Article-specific
  if (type === 'article') {
    if (author) setProperty('article:author', author)
    if (publishedTime) setProperty('article:published_time', publishedTime)
  }

  // Canonical
  if (url) setCanonical(url)
}

/**
 * Get the current page's full URL.
 */
export function getCurrentUrl() {
  return window.location.origin + window.location.pathname
}
