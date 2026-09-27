// 预置形象（头像）：以短 key 存库（如 preset:orange），渲染时映射为内联 SVG。
// 这样数据库只存十几个字符，前端却能得到清晰可缩放的矢量头像。

const face = (eyeY = 30, opts = {}) => {
  const { mouth = 'smile', eyeGap = 6, eyeR = 2.6, mouthY = eyeY + 9, white = false } = opts
  const left = 32 - eyeGap
  const right = 32 + eyeGap
  const ink = white ? '#fff' : '#111'
  const mouthSvg = mouth === 'line'
    ? `<line x1="${32 - 5}" y1="${mouthY}" x2="${32 + 5}" y2="${mouthY}" stroke="${ink}" stroke-width="2.4" stroke-linecap="round"/>`
    : mouth === 'none'
      ? ''
      : `<path d="M${32 - 5} ${mouthY} q5 5 10 0" fill="none" stroke="${ink}" stroke-width="2.4" stroke-linecap="round"/>`
  return `
    <circle cx="${left}" cy="${eyeY}" r="${eyeR}" fill="${ink}"/>
    <circle cx="${right}" cy="${eyeY}" r="${eyeR}" fill="${ink}"/>
    ${mouthSvg}`
}

const wrap = (inner, bg) => `
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64" width="64" height="64">
  <circle cx="32" cy="32" r="32" fill="${bg}"/>
  ${inner}
</svg>`

const SVGS = {
  // 橙色圆顶
  orange: wrap(`<path d="M8 46a24 24 0 0 1 48 0z" fill="#f97316"/>${face(36, { eyeGap: 8, eyeR: 3, mouthY: 46 })}`, '#fff1e6'),
  // 黑色方块
  black: wrap(`<rect x="16" y="13" width="32" height="39" rx="9" fill="#1f2937"/>${face(29, { eyeGap: 8, eyeR: 3, mouth: 'line', mouthY: 41, white: true })}`, '#eceff3'),
  // 黄色圆角
  yellow: wrap(`<rect x="12" y="19" width="40" height="29" rx="11" fill="#facc15"/>${face(32, { eyeGap: 9, eyeR: 3, mouth: 'line', mouthY: 41 })}`, '#fffbe6'),
  // 紫色高个
  purple: wrap(`<rect x="19" y="10" width="26" height="44" rx="9" fill="#8b5cf6"/>${face(24, { eyeGap: 6, eyeR: 2.6, mouth: 'none', white: true })}`, '#f3ecff'),
  // 青色圆形
  teal: wrap(`<circle cx="32" cy="34" r="20" fill="#14b8a6"/>${face(30, { eyeGap: 8, eyeR: 3, mouthY: 41 })}`, '#e6fbf8'),
}

const toDataUri = (svg) => `data:image/svg+xml,${encodeURIComponent(svg.replace(/\s+/g, ' ').trim())}`

/** 可选形象列表（顺序即界面展示顺序） */
export const PRESET_AVATARS = Object.keys(SVGS).map((key) => ({
  key: `preset:${key}`,
  url: toDataUri(SVGS[key]),
}))

const MAP = Object.fromEntries(PRESET_AVATARS.map((a) => [a.key, a.url]))

/**
 * 把数据库里的 avatar 字段转成可直接用于 <img src> 的地址。
 * - 空值 → ''（调用方回退到首字母头像）
 * - preset:xxx → 内联 SVG data URI
 * - 其它（http/相对路径）→ 原样返回
 */
export function avatarUrl(value) {
  if (!value) return ''
  if (value.startsWith('preset:')) return MAP[value] || ''
  return value
}

/** 后端校验用的白名单 key */
export const PRESET_KEYS = PRESET_AVATARS.map((a) => a.key)
