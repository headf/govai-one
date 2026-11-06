// WebSocket 大屏推送：走 Vite proxy 连后端 /ws，无 token 直连
// 后端广播 { event: 'approval.updated', instanceId } → 触发注册的回调

let socket = null
let reconnectTimer = null
const callbacks = new Set()
let manualClosed = false

function onMessage(ev) {
  try {
    const payload = JSON.parse(ev.data)
    callbacks.forEach((cb) => cb(payload))
  } catch (e) {
    // 非 JSON（如心跳）可忽略
  }
}

function connect() {
  const proto = location.protocol === 'https:' ? 'wss' : 'ws'
  const ws = new WebSocket(`${proto}://${location.host}/ws`)
  socket = ws
  ws.onopen = () => { console.log('[ws] connected') }
  ws.onmessage = onMessage
  ws.onclose = () => {
    socket = null
    clearTimeout(reconnectTimer)
    if (!manualClosed) {
      // 断线 5s 后重连
      reconnectTimer = setTimeout(connect, 5000)
    }
  }
  ws.onerror = () => ws.close()
}

// 建立连接（幂等）
export function wsConnect() {
  if (socket) return
  manualClosed = false
  connect()
}

// 手动关闭（主动注销后使用）
export function wsClose() {
  manualClosed = true
  if (socket) socket.close()
  socket = null
}

// 注册回调，返回注销函数
export function wsSubscribe(cb) {
  callbacks.add(cb)
  return () => callbacks.delete(cb)
}