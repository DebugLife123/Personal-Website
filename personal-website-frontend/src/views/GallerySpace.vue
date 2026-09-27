<template>
  <div class="space-page">
    <!-- three.js 画布 -->
    <div ref="canvasHost" class="canvas-host"></div>
    <!-- 四周压暗，让视线聚焦中央 -->
    <div class="vignette"></div>

    <!-- 顶部工具栏 -->
    <div class="top-bar">
      <button class="glass-btn" @click="goGrid">
        <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M19 12H5M11 18l-6-6 6-6" />
        </svg>
        返回网格
      </button>
      <div class="top-title">
        <span class="dot"></span>
        图库 · 沉浸模式
        <span class="count" v-if="photos.length">{{ photos.length }} 张</span>
      </div>
      <div class="top-right">
        <span class="hint">拖拽旋转 · 滚轮缩放 · 点击照片看详情</span>
      </div>
    </div>

    <!-- 加载进度 -->
    <transition name="fade">
      <div v-if="loading" class="loader">
        <div class="loader-ring"><span></span></div>
        <p class="loader-text">正在点亮照片墙… {{ loadedCount }} / {{ photos.length }}</p>
        <div class="loader-bar"><i :style="{ width: progress + '%' }"></i></div>
      </div>
    </transition>

    <!-- 空状态 -->
    <div v-if="!loading && photos.length === 0" class="empty">
      <p class="empty-title">图库还是空的</p>
      <p class="empty-sub">去后台上传几张照片，这里就会亮起来</p>
      <button class="glass-btn" @click="goGrid">返回网格</button>
    </div>

    <!-- 照片详情 -->
    <transition name="panel">
      <div v-if="active" class="detail" @click.self="closeDetail">
        <button class="close-btn" @click="closeDetail" title="关闭 (Esc)">
          <svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
            <path d="M6 6l12 12M18 6L6 18" />
          </svg>
        </button>
        <button v-if="photos.length > 1" class="nav-btn prev" @click.stop="step(-1)" title="上一张 (←)">
          <svg viewBox="0 0 24 24" width="19" height="19" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M15 6l-6 6 6 6" />
          </svg>
        </button>
        <button v-if="photos.length > 1" class="nav-btn next" @click.stop="step(1)" title="下一张 (→)">
          <svg viewBox="0 0 24 24" width="19" height="19" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M9 6l6 6-6 6" />
          </svg>
        </button>

        <div class="detail-panel" @click.stop>
          <div class="detail-image">
            <img :src="active.url" :alt="active.title || active.description" />
          </div>
          <div class="detail-side">
            <p class="detail-kicker">PHOTO · {{ String(activeIndex + 1).padStart(2, '0') }} / {{ photos.length }}</p>
            <h2 v-if="active.title" class="detail-title">{{ active.title }}</h2>
            <p class="detail-desc">{{ active.description || '这张照片还没有写简介' }}</p>
            <div class="detail-tags" v-if="active.location || active.shotTime">
              <span v-if="active.location" class="tag">{{ active.location }}</span>
              <span v-if="active.shotTime" class="tag">{{ active.shotTime }}</span>
            </div>
          </div>
        </div>
      </div>
    </transition>

    <!-- 音效开关（粒子/旋转） -->
    <div class="bottom-bar">
      <button class="glass-btn small" @click="toggleRotate" :title="autoRotate ? '暂停自动旋转' : '继续自动旋转'">
        <svg v-if="autoRotate" viewBox="0 0 24 24" width="14" height="14" fill="currentColor">
          <rect x="6" y="5" width="4" height="14" rx="1" /><rect x="14" y="5" width="4" height="14" rx="1" />
        </svg>
        <svg v-else viewBox="0 0 24 24" width="14" height="14" fill="currentColor">
          <path d="M8 5l11 7-11 7z" />
        </svg>
        {{ autoRotate ? '旋转中' : '已暂停' }}
      </button>
      <span class="fps-hint" v-if="fps">{{ fps }} FPS</span>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import request from '../utils/request'
import { getTheme } from '../utils/theme'

const router = useRouter()
const canvasHost = ref(null)

const photos = ref([])
const loading = ref(true)
const loadedCount = ref(0)
const active = ref(null)
const activeIndex = ref(-1)
const autoRotate = ref(true)
const fps = ref(0)

// ==================== three.js 场景 ====================
let THREE, OrbitControls
let renderer, scene, camera, controls, raycaster
let photoGroup, starGroup, glowMesh
let rafId = null
let disposed = false

const pointer = new (class { constructor() { this.x = 0; this.y = 0 } })()
let hovered = null
let focusTarget = null
let homeDistance = 470

const REDUCED_MOTION = typeof window !== 'undefined'
  && window.matchMedia?.('(prefers-reduced-motion: reduce)').matches

const IS_MOBILE = typeof window !== 'undefined' && window.innerWidth < 820

const progress = ref(0)

// ---- 工具：生成圆形粒子贴图 ----
const makeSpriteTexture = (T) => {
  const size = 64
  const c = document.createElement('canvas')
  c.width = c.height = size
  const ctx = c.getContext('2d')
  const g = ctx.createRadialGradient(size / 2, size / 2, 0, size / 2, size / 2, size / 2)
  g.addColorStop(0, 'rgba(255,255,255,1)')
  g.addColorStop(0.35, 'rgba(255,255,255,0.55)')
  g.addColorStop(1, 'rgba(255,255,255,0)')
  ctx.fillStyle = g
  ctx.fillRect(0, 0, size, size)
  const tex = new T.CanvasTexture(c)
  tex.colorSpace = T.SRGBColorSpace
  return tex
}

// ---- 工具：把照片画成圆角卡片（带细边框），失败时回退为原始贴图 ----
const makeCardTexture = (T, img) => {
  try {
    const maxSide = 1024
    const ratio = img.naturalWidth / img.naturalHeight || 4 / 3
    let w = ratio >= 1 ? maxSide : Math.round(maxSide * ratio)
    let h = ratio >= 1 ? Math.round(maxSide / ratio) : maxSide
    const pad = 26
    const canvas = document.createElement('canvas')
    canvas.width = w + pad * 2
    canvas.height = h + pad * 2
    const ctx = canvas.getContext('2d')

    // 卡片底 + 圆角
    const r = 26
    const roundRect = (x, y, ww, hh, rr) => {
      ctx.beginPath()
      ctx.moveTo(x + rr, y)
      ctx.arcTo(x + ww, y, x + ww, y + hh, rr)
      ctx.arcTo(x + ww, y + hh, x, y + hh, rr)
      ctx.arcTo(x, y + hh, x, y, rr)
      ctx.arcTo(x, y, x + ww, y, rr)
      ctx.closePath()
    }
    roundRect(0, 0, canvas.width, canvas.height, r)
    ctx.fillStyle = 'rgba(255,255,255,0.94)'
    ctx.fill()

    ctx.save()
    roundRect(pad - 6, pad - 6, w + 12, h + 12, r - 6)
    ctx.clip()
    ctx.drawImage(img, pad, pad, w, h)
    ctx.restore()

    // 细边框
    ctx.lineWidth = 3
    ctx.strokeStyle = 'rgba(20,26,40,0.16)'
    roundRect(1.5, 1.5, canvas.width - 3, canvas.height - 3, r)
    ctx.stroke()

    const tex = new T.CanvasTexture(canvas)
    tex.colorSpace = T.SRGBColorSpace
    tex.anisotropy = 4
    return { tex, aspect: canvas.width / canvas.height }
  } catch (e) {
    // 跨域图片会污染 canvas：回退为直接使用图片贴图
    const tex = new T.Texture(img)
    tex.colorSpace = T.SRGBColorSpace
    tex.needsUpdate = true
    return { tex, aspect: img.naturalWidth / img.naturalHeight || 4 / 3 }
  }
}

const loadImage = (url) => new Promise((resolve) => {
  const img = new Image()
  img.crossOrigin = 'anonymous'
  img.onload = () => resolve(img)
  img.onerror = () => {
    // 跨域无 CORS 时，退回不带 crossOrigin 加载
    const img2 = new Image()
    img2.onload = () => resolve(img2)
    img2.onerror = () => resolve(null)
    img2.src = url
  }
  img.src = url
})

const initScene = () => {
  // ---- 渲染器 ----
  renderer = new THREE.WebGLRenderer({ antialias: !IS_MOBILE, alpha: false, powerPreference: 'high-performance' })
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, IS_MOBILE ? 1.5 : 2))
  renderer.setSize(canvasHost.value.clientWidth, canvasHost.value.clientHeight)
  renderer.setClearColor(0x04060d, 1)
  canvasHost.value.appendChild(renderer.domElement)

  // ---- 场景与相机 ----
  scene = new THREE.Scene()
  scene.fog = new THREE.FogExp2(0x04060d, 0.0011)

  camera = new THREE.PerspectiveCamera(
    58,
    canvasHost.value.clientWidth / canvasHost.value.clientHeight,
    1,
    4000
  )
  camera.position.set(0, 40, homeDistance)

  controls = new OrbitControls(camera, renderer.domElement)
  controls.enableDamping = true
  controls.dampingFactor = 0.06
  controls.enablePan = false
  controls.minDistance = 190
  controls.maxDistance = 900
  controls.rotateSpeed = 0.55
  controls.zoomSpeed = 0.7
  controls.autoRotate = autoRotate.value && !REDUCED_MOTION
  controls.autoRotateSpeed = 0.32

  raycaster = new THREE.Raycaster()

  // ---- 星空粒子（两层，营造纵深） ----
  const sprite = makeSpriteTexture(THREE)
  starGroup = new THREE.Group()
  scene.add(starGroup)

  const buildStars = (count, inner, outer, size, opacity, color) => {
    const geo = new THREE.BufferGeometry()
    const positions = new Float32Array(count * 3)
    for (let i = 0; i < count; i++) {
      // 球壳内均匀分布
      const u = Math.random() * 2 - 1
      const theta = Math.random() * Math.PI * 2
      const r = inner + Math.random() * (outer - inner)
      const s = Math.sqrt(1 - u * u)
      positions[i * 3] = r * s * Math.cos(theta)
      positions[i * 3 + 1] = r * u * 0.72
      positions[i * 3 + 2] = r * s * Math.sin(theta)
    }
    geo.setAttribute('position', new THREE.BufferAttribute(positions, 3))
    const mat = new THREE.PointsMaterial({
      size,
      map: sprite,
      color,
      transparent: true,
      opacity,
      depthWrite: false,
      blending: THREE.AdditiveBlending,
      sizeAttenuation: true
    })
    const points = new THREE.Points(geo, mat)
    starGroup.add(points)
    return points
  }

  buildStars(IS_MOBILE ? 1100 : 2800, 320, 1600, 4.5, 0.8, 0xffffff)
  buildStars(IS_MOBILE ? 500 : 1200, 260, 1200, 11, 0.38, 0x9fc4ff)

  // ---- 中央光束 + 柔光核心 ----
  const beamCanvas = document.createElement('canvas')
  beamCanvas.width = 128
  beamCanvas.height = 512
  const bctx = beamCanvas.getContext('2d')
  const bg = bctx.createLinearGradient(0, 0, 0, 512)
  bg.addColorStop(0, 'rgba(255,255,255,0)')
  bg.addColorStop(0.44, 'rgba(206,222,255,0.5)')
  bg.addColorStop(0.5, 'rgba(255,255,255,0.9)')
  bg.addColorStop(0.56, 'rgba(206,222,255,0.45)')
  bg.addColorStop(1, 'rgba(255,255,255,0)')
  bctx.fillStyle = bg
  bctx.fillRect(0, 0, 128, 512)
  // 横向羽化掉左右硬边，避免出现"灰色色带"
  bctx.globalCompositeOperation = 'destination-in'
  const hg = bctx.createLinearGradient(0, 0, 128, 0)
  hg.addColorStop(0, 'rgba(0,0,0,0)')
  hg.addColorStop(0.5, 'rgba(0,0,0,1)')
  hg.addColorStop(1, 'rgba(0,0,0,0)')
  bctx.fillStyle = hg
  bctx.fillRect(0, 0, 128, 512)
  bctx.globalCompositeOperation = 'source-over'

  const beamTex = new THREE.CanvasTexture(beamCanvas)
  beamTex.colorSpace = THREE.SRGBColorSpace
  const beamGeo = new THREE.PlaneGeometry(120, 1600)
  const beamMat = new THREE.MeshBasicMaterial({
    map: beamTex,
    transparent: true,
    opacity: 0.22,
    blending: THREE.AdditiveBlending,
    depthWrite: false,
    side: THREE.DoubleSide,
    fog: false
  })
  glowMesh = new THREE.Mesh(beamGeo, beamMat)

  // 中心柔光核心（billboard 光晕）
  const coreCanvas = document.createElement('canvas')
  coreCanvas.width = coreCanvas.height = 256
  const cctx = coreCanvas.getContext('2d')
  const cg = cctx.createRadialGradient(128, 128, 0, 128, 128, 128)
  cg.addColorStop(0, 'rgba(255,255,255,0.55)')
  cg.addColorStop(0.25, 'rgba(198,216,255,0.28)')
  cg.addColorStop(0.6, 'rgba(120,150,235,0.08)')
  cg.addColorStop(1, 'rgba(80,110,200,0)')
  cctx.fillStyle = cg
  cctx.fillRect(0, 0, 256, 256)
  const coreTex = new THREE.CanvasTexture(coreCanvas)
  coreTex.colorSpace = THREE.SRGBColorSpace
  const coreMat = new THREE.SpriteMaterial({
    map: coreTex,
    transparent: true,
    blending: THREE.AdditiveBlending,
    depthWrite: false,
    fog: false
  })
  const core = new THREE.Sprite(coreMat)
  core.scale.set(1500, 1500, 1)
  // 放到深处当"星云核心"，避免把中间的卡片照白
  core.position.set(0, -60, -900)

  const glowGroup = new THREE.Group()
  glowGroup.add(glowMesh)
  glowGroup.add(core)
  scene.add(glowGroup)
  glowMesh.userData.isBeam = true

  photoGroup = new THREE.Group()
  scene.add(photoGroup)
}

// ---- 把照片按斐波那契球面散布 ----
const buildPhotos = (items) => {
  const n = items.length
  const golden = Math.PI * (3 - Math.sqrt(5))
  // 照片少时收拢半径并放大，画面不至于空旷
  const sparse = n <= 8
  const radius = sparse ? 240 : 300
  const maxDim = sparse ? (IS_MOBILE ? 132 : 158) : (IS_MOBILE ? 118 : 132)

  items.forEach((item, i) => {
    const y = 1 - (i / Math.max(n - 1, 1)) * 2
    const r = Math.sqrt(Math.max(0, 1 - y * y))
    const theta = golden * i
    const jitter = 0.86 + Math.random() * 0.28
    const px = Math.cos(theta) * r * radius * jitter
    const py = y * radius * 0.62 * jitter
    const pz = Math.sin(theta) * r * radius * jitter

    const { tex, aspect } = item.card
    const w = aspect >= 1 ? maxDim : maxDim * aspect
    const h = aspect >= 1 ? maxDim / aspect : maxDim

    const mesh = new THREE.Mesh(
      new THREE.PlaneGeometry(w, h),
      new THREE.MeshBasicMaterial({
        map: tex,
        transparent: true,
        side: THREE.DoubleSide,
        depthWrite: false,
        opacity: 0
      })
    )
    mesh.position.set(px, py, pz)
    // 朝向球外：相机在球壳外侧，这样看到的是正面（否则带文字的图会镜像）
    mesh.lookAt(px * 2, py * 2, pz * 2)
    mesh.userData = {
      photo: item.photo,
      baseScale: 1,
      targetScale: 1,
      appearAt: i * 45
    }
    photoGroup.add(mesh)
  })

  // 逐张淡入
  const start = performance.now()
  const reveal = () => {
    if (disposed) return
    const t = performance.now() - start
    let done = true
    photoGroup.children.forEach((m) => {
      const p = Math.min(1, Math.max(0, (t - m.userData.appearAt) / 700))
      if (p < 1) done = false
      m.material.opacity = p
      m.scale.setScalar(0.72 + 0.28 * p)
    })
    if (!done) requestAnimationFrame(reveal)
  }
  reveal()
}

// ---- 交互 ----
const updatePointer = (e) => {
  const rect = renderer.domElement.getBoundingClientRect()
  pointer.x = ((e.clientX - rect.left) / rect.width) * 2 - 1
  pointer.y = -((e.clientY - rect.top) / rect.height) * 2 + 1
}

const pickPhoto = () => {
  raycaster.setFromCamera(new THREE.Vector2(pointer.x, pointer.y), camera)
  const hits = raycaster.intersectObjects(photoGroup.children, false)
  return hits.length ? hits[0].object : null
}

const onPointerMove = (e) => {
  if (!renderer) return
  updatePointer(e)
  const hit = pickPhoto()
  if (hovered && hovered !== hit) hovered.userData.targetScale = 1
  if (hit) hit.userData.targetScale = 1.14
  hovered = hit
  renderer.domElement.style.cursor = hit ? 'pointer' : 'grab'
}

let downPos = null
const onPointerDown = (e) => { downPos = { x: e.clientX, y: e.clientY } }

const onPointerUp = (e) => {
  if (!downPos) return
  const moved = Math.hypot(e.clientX - downPos.x, e.clientY - downPos.y)
  downPos = null
  if (moved > 6) return // 拖拽不算点击
  if (!renderer) return
  updatePointer(e)
  const hit = pickPhoto()
  if (hit) openDetail(hit.userData.photo, hit)
  else if (active.value) closeDetail()
}

const onResize = () => {
  if (!renderer || !canvasHost.value) return
  const w = canvasHost.value.clientWidth
  const h = canvasHost.value.clientHeight
  camera.aspect = w / h
  camera.updateProjectionMatrix()
  renderer.setSize(w, h)
}

const onKey = (e) => {
  if (e.key === 'Escape' && active.value) closeDetail()
  else if (e.key === 'ArrowLeft' && active.value) step(-1)
  else if (e.key === 'ArrowRight' && active.value) step(1)
}

// ---- 详情 ----
const openDetail = (photo, mesh) => {
  active.value = photo
  activeIndex.value = photos.value.findIndex((p) => p.id === photo.id)
  focusTarget = mesh ? mesh.getWorldPosition(new THREE.Vector3()) : null
  controls.autoRotate = false
  autoRotate.value = false
}

const closeDetail = () => {
  active.value = null
  activeIndex.value = -1
  focusTarget = null
  if (!REDUCED_MOTION) {
    controls.autoRotate = true
    autoRotate.value = true
  }
}

const step = (delta) => {
  const total = photos.value.length
  if (!total) return
  const next = (activeIndex.value + delta + total) % total
  const photo = photos.value[next]
  activeIndex.value = next
  active.value = photo
  const mesh = photoGroup?.children.find((m) => m.userData.photo?.id === photo.id)
  focusTarget = mesh ? mesh.getWorldPosition(new THREE.Vector3()) : null
}

const toggleRotate = () => {
  autoRotate.value = !autoRotate.value
  if (controls) controls.autoRotate = autoRotate.value
}

// ---- 渲染循环 ----
let lastFpsAt = performance.now()
let frames = 0

const animate = () => {
  if (disposed) return
  rafId = requestAnimationFrame(animate)

  const now = performance.now()
  // 帧率统计
  frames++
  if (now - lastFpsAt > 1000) {
    fps.value = Math.round((frames * 1000) / (now - lastFpsAt))
    frames = 0
    lastFpsAt = now
  }

  // 照片：悬停缩放 + 呼吸漂浮
  const t = now * 0.001
  photoGroup.children.forEach((m, i) => {
    const s = m.userData.targetScale
    m.scale.lerp(new THREE.Vector3(s, s, s), 0.12)
    m.position.y += Math.sin(t * 0.6 + i) * 0.012
  })

  // 星海缓慢自转
  starGroup.rotation.y += 0.00035
  starGroup.rotation.x = Math.sin(t * 0.08) * 0.02

  // 光束始终面向相机
  if (glowMesh) glowMesh.quaternion.copy(camera.quaternion)

  // 聚焦某张照片时轻推相机
  if (focusTarget) {
    controls.target.lerp(focusTarget, 0.05)
    const dir = focusTarget.clone().normalize()
    const desired = focusTarget.clone().add(dir.multiplyScalar(210))
    camera.position.lerp(desired, 0.035)
  } else {
    controls.target.lerp(new THREE.Vector3(0, 0, 0), 0.03)
  }

  controls.update()
  renderer.render(scene, camera)
}

// ==================== 生命周期 ====================
const goGrid = () => router.push('/gallery')

const bootstrap = async () => {
  THREE = await import('three')
  const mod = await import('three/examples/jsm/controls/OrbitControls.js')
  OrbitControls = mod.OrbitControls

  // 拉取照片
  let items = []
  try {
    const res = await request.get('/gallery/list')
    if (res.data.code === 200) {
      items = (res.data.data || []).filter((p) => p.url).slice(0, 80)
    }
  } catch (e) {
    console.error('获取图库失败', e)
  }
  photos.value = items

  if (!items.length) {
    loading.value = false
    return
  }

  // 并发预载图片（限制并发，避免一次打满）
  const queue = [...items]
  const results = []
  const worker = async () => {
    while (queue.length) {
      const photo = queue.shift()
      const img = await loadImage(photo.url)
      loadedCount.value++
      progress.value = Math.round((loadedCount.value / items.length) * 100)
      if (img) results.push({ photo, img })
    }
  }
  await Promise.all(Array.from({ length: Math.min(6, items.length) }, worker))

  if (disposed) return

  await nextTick()
  initScene()
  const withCards = results.map(({ photo, img }) => ({ photo, card: makeCardTexture(THREE, img) }))
  withCards.sort((a, b) => (a.photo.sort ?? 0) - (b.photo.sort ?? 0))
  buildPhotos(withCards)

  animate()
  loading.value = false

  window.addEventListener('resize', onResize)
  window.addEventListener('keydown', onKey)
  const el = renderer.domElement
  el.addEventListener('pointermove', onPointerMove)
  el.addEventListener('pointerdown', onPointerDown)
  el.addEventListener('pointerup', onPointerUp)
  el.style.cursor = 'grab'
}

onMounted(bootstrap)

onBeforeUnmount(() => {
  disposed = true
  if (rafId) cancelAnimationFrame(rafId)
  window.removeEventListener('resize', onResize)
  window.removeEventListener('keydown', onKey)
  try {
    if (renderer) {
      renderer.domElement.removeEventListener('pointermove', onPointerMove)
      renderer.domElement.removeEventListener('pointerdown', onPointerDown)
      renderer.domElement.removeEventListener('pointerup', onPointerUp)
    }
    // 释放 GPU 资源
    if (photoGroup) {
      photoGroup.children.forEach((m) => {
        m.geometry?.dispose()
        m.material?.map?.dispose()
        m.material?.dispose()
      })
    }
    if (starGroup) {
      starGroup.children.forEach((p) => {
        p.geometry?.dispose()
        p.material?.map?.dispose()
        p.material?.dispose()
      })
    }
    if (glowMesh) {
      glowMesh.geometry?.dispose()
      glowMesh.material?.map?.dispose()
      glowMesh.material?.dispose()
      // 中心柔光精灵
      const core = glowMesh.parent?.children?.find((c) => c.isSprite)
      if (core) {
        core.material?.map?.dispose()
        core.material?.dispose()
      }
    }
    controls?.dispose()
    renderer?.dispose()
    if (renderer?.domElement?.parentNode) {
      renderer.domElement.parentNode.removeChild(renderer.domElement)
    }
  } catch (e) {
    console.warn('清理 three.js 资源时出错', e)
  }
})
</script>

<style scoped>
.space-page {
  position: relative;
  width: 100%;
  height: 100vh;
  overflow: hidden;
  background: #04060d;
  color: #eef2fb;
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Display", "Helvetica Neue",
    "PingFang SC", "Microsoft YaHei", sans-serif;
  -webkit-font-smoothing: antialiased;
}

.canvas-host { position: absolute; inset: 0; }
.canvas-host :deep(canvas) { display: block; }

.vignette {
  position: absolute;
  inset: 0;
  z-index: 2;
  pointer-events: none;
  background: radial-gradient(
    ellipse 78% 68% at 50% 48%,
    rgba(0, 0, 0, 0) 42%,
    rgba(2, 4, 10, 0.45) 78%,
    rgba(2, 4, 10, 0.78) 100%
  );
}

/* ---------- 顶部栏 ---------- */
.top-bar {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  z-index: 5;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 24px;
  background: linear-gradient(180deg, rgba(4, 6, 13, 0.72) 0%, rgba(4, 6, 13, 0) 100%);
  pointer-events: none;
}

.top-bar > * { pointer-events: auto; }

.top-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 0.9rem;
  letter-spacing: 2px;
  color: rgba(238, 242, 251, 0.9);
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #7ba3f5;
  box-shadow: 0 0 10px 2px rgba(123, 163, 245, 0.8);
  animation: pulse 2.2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 0.5; transform: scale(0.85); }
  50% { opacity: 1; transform: scale(1.15); }
}

.count {
  padding: 2px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.14);
  font-size: 0.72rem;
  letter-spacing: 1px;
  color: rgba(238, 242, 251, 0.75);
}

.hint {
  font-size: 0.76rem;
  letter-spacing: 1px;
  color: rgba(238, 242, 251, 0.42);
}

.glass-btn {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 9px 16px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(14px);
  color: rgba(238, 242, 251, 0.9);
  font-family: inherit;
  font-size: 0.8rem;
  letter-spacing: 0.5px;
  cursor: pointer;
  transition: background 0.2s, transform 0.2s, border-color 0.2s;
}

.glass-btn:hover {
  background: rgba(255, 255, 255, 0.16);
  border-color: rgba(255, 255, 255, 0.3);
  transform: translateY(-1px);
}

.glass-btn.small { padding: 7px 13px; font-size: 0.74rem; }

/* ---------- 底部栏 ---------- */
.bottom-bar {
  position: absolute;
  left: 24px;
  bottom: 20px;
  z-index: 5;
  display: flex;
  align-items: center;
  gap: 12px;
}

.fps-hint {
  font-size: 0.7rem;
  letter-spacing: 1px;
  color: rgba(238, 242, 251, 0.3);
}

/* ---------- 加载 ---------- */
.loader {
  position: absolute;
  inset: 0;
  z-index: 8;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 18px;
  background: radial-gradient(700px 460px at 50% 45%, #0d1430 0%, #04060d 70%);
}

.loader-ring {
  width: 62px;
  height: 62px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.1);
  border-top-color: #7ba3f5;
  border-right-color: rgba(123, 163, 245, 0.45);
  animation: spin 0.9s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

.loader-text {
  margin: 0;
  font-size: 0.8rem;
  letter-spacing: 2px;
  color: rgba(238, 242, 251, 0.6);
}

.loader-bar {
  width: 190px;
  height: 2px;
  border-radius: 2px;
  background: rgba(255, 255, 255, 0.1);
  overflow: hidden;
}

.loader-bar i {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, #4f7ff0, #9fc4ff);
  transition: width 0.3s ease;
}

.fade-enter-active, .fade-leave-active { transition: opacity 0.5s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* ---------- 空状态 ---------- */
.empty {
  position: absolute;
  inset: 0;
  z-index: 6;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  text-align: center;
}

.empty-title { margin: 0; font-size: 1.05rem; letter-spacing: 1px; }
.empty-sub { margin: 0 0 10px; font-size: 0.82rem; color: rgba(238, 242, 251, 0.45); }

/* ---------- 详情 ---------- */
.detail {
  position: absolute;
  inset: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
  background: rgba(3, 5, 11, 0.62);
  backdrop-filter: blur(8px);
}

.detail-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  width: min(1160px, 100%);
  max-height: calc(100vh - 96px);
  border-radius: 18px;
  overflow: hidden;
  background: rgba(12, 16, 28, 0.96);
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 40px 100px -30px rgba(0, 0, 0, 0.85);
}

.detail-image {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #05070e;
  min-height: 260px;
}

.detail-image img {
  max-width: 100%;
  max-height: calc(100vh - 96px);
  object-fit: contain;
  display: block;
}

.detail-side {
  padding: 30px 28px;
  overflow-y: auto;
  border-left: 1px solid rgba(255, 255, 255, 0.08);
}

.detail-kicker {
  margin: 0 0 12px;
  font-size: 0.68rem;
  letter-spacing: 2.4px;
  color: #7ba3f5;
}

.detail-title {
  margin: 0 0 14px;
  font-size: 1.24rem;
  font-weight: 800;
  letter-spacing: 0.5px;
  color: #f4f7ff;
}

.detail-desc {
  margin: 0;
  font-size: 0.88rem;
  line-height: 1.9;
  color: rgba(238, 242, 251, 0.72);
  white-space: pre-wrap;
}

.detail-tags { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 20px; }

.tag {
  padding: 4px 11px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(255, 255, 255, 0.06);
  font-size: 0.72rem;
  letter-spacing: 0.5px;
  color: rgba(238, 242, 251, 0.72);
}

.close-btn, .nav-btn {
  position: absolute;
  width: 42px;
  height: 42px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(12px);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s, transform 0.2s;
}

.close-btn { top: 26px; right: 28px; }
.close-btn:hover { background: rgba(255, 255, 255, 0.2); transform: rotate(90deg); }

.nav-btn { top: 50%; margin-top: -21px; }
.nav-btn.prev { left: 22px; }
.nav-btn.next { right: 22px; }
.nav-btn:hover { background: rgba(255, 255, 255, 0.22); }

.panel-enter-active, .panel-leave-active { transition: opacity 0.3s ease, transform 0.3s ease; }
.panel-enter-from, .panel-leave-to { opacity: 0; }
.panel-enter-from .detail-panel, .panel-leave-to .detail-panel { transform: translateY(16px) scale(0.99); }

/* ---------- 响应式 ---------- */
@media (max-width: 900px) {
  .hint, .fps-hint { display: none; }
  .top-bar { padding: 14px 16px; }
  .detail { padding: 16px; }
  .detail-panel { grid-template-columns: 1fr; max-height: calc(100vh - 32px); }
  .detail-image { min-height: 180px; }
  .detail-side { border-left: none; border-top: 1px solid rgba(255, 255, 255, 0.08); padding: 20px; }
  .nav-btn.prev { left: 8px; }
  .nav-btn.next { right: 8px; }
}

@media (prefers-reduced-motion: reduce) {
  .dot { animation: none; }
}
</style>
