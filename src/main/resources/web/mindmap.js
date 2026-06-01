const Mindmap = (() => {
  // ── constants ─────────────────────────────────────────────────────────
  const NODE_WIDTH = 180;
  const NODE_HEIGHT = 56;
  const LAYER_GAP = 240;
  const NODE_GAP = 40;
  const START_X = 40;
  const START_Y = 40;

  // ── view state ────────────────────────────────────────────────────────
  function createViewState() {
    return {
      zoom: 1.0,
      panX: 0, panY: 0,
      hoveredNode: null,
      selectedNode: null,
      nodes: [],
    };
  }

  function screenToWorld(sx, sy, vs) {
    return {
      x: (sx - vs.panX) / vs.zoom,
      y: (sy - vs.panY) / vs.zoom,
    };
  }

  function worldToScreen(wx, wy, vs) {
    return {
      x: wx * vs.zoom + vs.panX,
      y: wy * vs.zoom + vs.panY,
    };
  }

  // ── layout algorithm ──────────────────────────────────────────────────
  /**
   * @param {Array} quests — [{id, npc_id, prerequisites: string[]}, ...]
   * @returns {Array} nodes — [{id, x, y, width, height, prereqs, npcId}, ...]
   */
  function layoutNodes(quests) {
    if (!quests || quests.length === 0) return [];

    const questSet = new Set(quests.map(q => q.id));
    const questMap = {};
    for (const q of quests) questMap[q.id] = q;

    // adjacency: prereq → [dependents] (only quests within the set)
    const children = {};
    const inDegree = {};

    for (const q of quests) {
      children[q.id] = [];
      // count only prerequisites that are in the current quest set
      const activePrereqs = q.prerequisites ? q.prerequisites.filter(p => questSet.has(p)) : [];
      inDegree[q.id] = activePrereqs.length;
    }

    for (const q of quests) {
      const activePrereqs = q.prerequisites ? q.prerequisites.filter(p => questSet.has(p)) : [];
      for (const prereq of activePrereqs) {
        if (!children[prereq]) children[prereq] = [];
        children[prereq].push(q.id);
      }
    }

    // Kahn's BFS
    const queue = [];
    const layers = {};

    for (const q of quests) {
      if (inDegree[q.id] === 0) queue.push(q.id);
    }

    while (queue.length > 0) {
      const id = queue.shift();

      // layer = max(prereq layers) + 1, or 0 if no active prereqs
      const q = questMap[id];
      const activePrereqs = q.prerequisites ? q.prerequisites.filter(p => questSet.has(p)) : [];
      let maxPrereqLayer = -1;
      for (const p of activePrereqs) {
        if (layers[p] !== undefined && layers[p] > maxPrereqLayer) {
          maxPrereqLayer = layers[p];
        }
      }
      layers[id] = maxPrereqLayer + 1;

      for (const child of (children[id] || [])) {
        inDegree[child]--;
        if (inDegree[child] === 0) queue.push(child);
      }
    }

    // safeguard: unreachable nodes (cycles) → layer 0
    for (const q of quests) {
      if (layers[q.id] === undefined) layers[q.id] = 0;
    }

    // group by layer
    const layerGroups = {};
    for (const q of quests) {
      const l = layers[q.id];
      if (!layerGroups[l]) layerGroups[l] = [];
      layerGroups[l].push(q);
    }

    // assign (x, y) positions
    const nodes = [];
    const sortedLayers = Object.keys(layerGroups)
      .map(Number)
      .sort((a, b) => a - b);

    for (const layer of sortedLayers) {
      const layerNodes = layerGroups[layer];
      layerNodes.forEach((q, i) => {
        nodes.push({
          id: q.id,
          name: q.name || '',
          x: START_X + layer * LAYER_GAP,
          y: START_Y + i * (NODE_HEIGHT + NODE_GAP),
          width: NODE_WIDTH,
          height: NODE_HEIGHT,
          prereqs: q.prerequisites ? q.prerequisites.filter(p => questSet.has(p)) : [],
          npcId: q.npc_id || '',
          npcName: q.npc_name || '',
        });
      });
    }

    return nodes;
  }

  // ── colours (read CSS vars once) ────────────────────────────────────────
  let TEXT_COLOR = '#1a1d23';
  let TEXT_DIM_COLOR = '#687180';

  function _readColors() {
    const style = getComputedStyle(document.body);
    TEXT_COLOR = style.getPropertyValue('--text').trim() || TEXT_COLOR;
    TEXT_DIM_COLOR = style.getPropertyValue('--text-dim').trim() || TEXT_DIM_COLOR;
  }

  // ── text helpers ───────────────────────────────────────────────────────
  function _shortenId(fullId, maxPx, ctx) {
    // strip namespace prefix: "surezs_quest:collect_iron" → "collect_iron"
    const colon = fullId.lastIndexOf(':');
    const short = colon > 0 ? fullId.substring(colon + 1) : fullId;
    if (!ctx) return short;
    if (ctx.measureText(short).width <= maxPx) return short;
    // truncate with ellipsis
    let trimmed = short;
    while (trimmed.length > 3 && ctx.measureText(trimmed + '...').width > maxPx) {
      trimmed = trimmed.slice(0, -1);
    }
    return trimmed + '...';
  }

  // ── canvas rendering ───────────────────────────────────────────────────
  function _resizeCanvas(canvas) {
    const wrap = canvas.parentElement;
    if (!wrap) return { w: 800, h: 600 };
    const dpr = window.devicePixelRatio || 1;
    const w = wrap.clientWidth;
    const h = wrap.clientHeight;
    if (canvas.width !== w * dpr || canvas.height !== h * dpr) {
      canvas.width = w * dpr;
      canvas.height = h * dpr;
      canvas.style.width = w + 'px';
      canvas.style.height = h + 'px';
    }
    return { w, h, dpr };
  }

  function _drawEdge(ctx, from, to) {
    const x1 = from.x + from.width;
    const y1 = from.y + from.height / 2;
    const x2 = to.x;
    const y2 = to.y + to.height / 2;
    const cx = (x1 + x2) / 2;

    ctx.beginPath();
    ctx.moveTo(x1, y1);
    ctx.bezierCurveTo(cx, y1, cx, y2, x2, y2);
    ctx.strokeStyle = 'rgba(59,130,196,0.3)';
    ctx.lineWidth = 2;
    ctx.stroke();

    // arrowhead at (x2, y2) pointing right
    ctx.beginPath();
    ctx.moveTo(x2, y2);
    ctx.lineTo(x2 - 6, y2 + 4);
    ctx.lineTo(x2 - 6, y2 - 4);
    ctx.closePath();
    ctx.fillStyle = 'rgba(59,130,196,0.4)';
    ctx.fill();
  }

  function _drawNode(ctx, node, vs) {
    const { x, y, width, height, id, name, npcId, npcName } = node;
    const isHovered = vs.hoveredNode && vs.hoveredNode.id === id;
    const isSelected = vs.selectedNode && vs.selectedNode.id === id;
    const radius = 8;

    // shadow
    ctx.shadowColor = isHovered ? 'rgba(59,130,196,0.15)' : 'rgba(0,0,0,0.06)';
    ctx.shadowBlur = isHovered ? 12 : 4;
    ctx.shadowOffsetX = 0;
    ctx.shadowOffsetY = isHovered ? 2 : 1;

    // background
    ctx.beginPath();
    ctx.roundRect(x, y, width, height, radius);
    ctx.fillStyle = 'rgba(255,255,255,0.75)';
    ctx.fill();

    // border
    ctx.shadowColor = 'transparent';
    ctx.shadowBlur = 0;
    if (isSelected) {
      ctx.strokeStyle = '#3b82c4';
      ctx.lineWidth = 2;
    } else if (isHovered) {
      ctx.strokeStyle = 'rgba(59,130,196,0.5)';
      ctx.lineWidth = 2;
    } else {
      ctx.strokeStyle = 'rgba(0,0,0,0.08)';
      ctx.lineWidth = 1;
    }
    ctx.stroke();

    // title — quest name (fallback: short id)
    ctx.font = '12px "JetBrains Mono", "Fira Code", monospace';
    ctx.fillStyle = TEXT_COLOR;
    const titleMaxW = width - 20;
    const title = name || _shortenId(id, titleMaxW, ctx);
    ctx.fillText(title, x + 10, y + 20);

    // subtitle — NPC name (fallback: short id)
    if (npcId) {
      ctx.font = '10px "JetBrains Mono", "Fira Code", monospace';
      ctx.fillStyle = TEXT_DIM_COLOR;
      const sub = npcName || _shortenId(npcId, titleMaxW, ctx);
      ctx.fillText(sub, x + 10, y + 40);
    }
  }

  function _drawEdges(ctx, nodes) {
    const nodeMap = {};
    for (const n of nodes) nodeMap[n.id] = n;
    for (const n of nodes) {
      for (const prereqId of n.prereqs) {
        const from = nodeMap[prereqId];
        if (from) _drawEdge(ctx, from, n);
      }
    }
  }

  function drawMindmap(ctx, nodes, vs) {
    _drawEdges(ctx, nodes);
    for (const n of nodes) _drawNode(ctx, n, vs);
  }

  // ── hit testing ───────────────────────────────────────────────────────
  function findNodeAt(sx, sy, vs) {
    if (!vs.nodes) return null;
    const p = screenToWorld(sx, sy, vs);
    // iterate in reverse so top-drawn node is checked first
    for (let i = vs.nodes.length - 1; i >= 0; i--) {
      const n = vs.nodes[i];
      if (p.x >= n.x && p.x <= n.x + n.width &&
          p.y >= n.y && p.y <= n.y + n.height) {
        return n;
      }
    }
    return null;
  }

  // ── context menu ──────────────────────────────────────────────────────
  let _ctxMenu = null;

  function _ensureCtxMenu() {
    if (_ctxMenu) return _ctxMenu;
    _ctxMenu = document.createElement('div');
    _ctxMenu.className = 'mindmap-ctx-menu';
    Object.assign(_ctxMenu.style, {
      position: 'absolute',
      display: 'none',
      zIndex: '1000',
      background: 'rgba(255,255,255,0.95)',
      backdropFilter: 'blur(12px)',
      WebkitBackdropFilter: 'blur(12px)',
      borderRadius: '10px',
      boxShadow: '0 8px 30px rgba(0,0,0,0.15)',
      border: '1px solid rgba(0,0,0,0.08)',
      padding: '4px',
      minWidth: '160px',
      overflow: 'hidden',
    });
    document.body.appendChild(_ctxMenu);
    return _ctxMenu;
  }

  function _showCtxMenu(e, node, callbacks) {
    if (!callbacks.getContextMenuItems) return;
    const items = callbacks.getContextMenuItems(node);
    if (!items || items.length === 0) return;

    const menu = _ensureCtxMenu();
    menu.innerHTML = '';
    for (const item of items) {
      const btn = document.createElement('button');
      btn.textContent = item.label;
      Object.assign(btn.style, {
        display: 'block',
        width: '100%',
        textAlign: 'left',
        padding: '6px 12px',
        fontSize: '0.82rem',
        border: 'none',
        borderRadius: '6px',
        background: 'none',
        cursor: 'pointer',
        color: 'var(--text)',
        fontFamily: 'system-ui, -apple-system, sans-serif',
      });
      btn.addEventListener('mouseenter', () => { btn.style.background = 'rgba(59,130,196,0.08)'; });
      btn.addEventListener('mouseleave', () => { btn.style.background = 'none'; });
      btn.addEventListener('click', () => {
        _hideCtxMenu();
        if (item.action) item.action(node);
      });
      menu.appendChild(btn);
    }

    // position within canvas container
    const container = e.target.parentElement;
    const rect = container.getBoundingClientRect();
    const left = rect.left + e.offsetX;
    const top = rect.top + e.offsetY;
    menu.style.left = Math.min(left, rect.right - 170) + 'px';
    menu.style.top = Math.min(top, rect.bottom - 100) + 'px';
    menu.style.display = 'block';
    menu._node = node;
  }

  function _hideCtxMenu() {
    if (_ctxMenu) _ctxMenu.style.display = 'none';
  }

  // ── events ────────────────────────────────────────────────────────────
  function attachEvents(canvas, vs, callbacks) {
    _readColors();

    const ctx = canvas.getContext('2d');
    let dragInfo = null;    // { node, startX, startY, origX, origY, dragging:bool }
    let panInfo = null;     // { startX, startY, startPanX, startPanY }

    function redraw() {
      const size = _resizeCanvas(canvas);
      // DPR base transform
      ctx.setTransform(size.dpr, 0, 0, size.dpr, 0, 0);
      ctx.clearRect(0, 0, size.w, size.h);

      if (callbacks.onBeforeRedraw) callbacks.onBeforeRedraw(vs);

      // zoom/pan transform
      ctx.save();
      ctx.translate(vs.panX, vs.panY);
      ctx.scale(vs.zoom, vs.zoom);
      drawMindmap(ctx, vs.nodes, vs);
      ctx.restore();
    }

    function screenOffset(e) {
      const rect = canvas.getBoundingClientRect();
      return { x: e.clientX - rect.left, y: e.clientY - rect.top };
    }

    canvas.addEventListener('mousemove', (e) => {
      const { x, y } = screenOffset(e);

      if (dragInfo) {
        if (!dragInfo.dragging) {
          const dx = x - dragInfo.startX;
          const dy = y - dragInfo.startY;
          if (Math.abs(dx) > 3 || Math.abs(dy) > 3) {
            dragInfo.dragging = true;
            canvas.style.cursor = 'grabbing';
          }
        }
        if (dragInfo.dragging) {
          dragInfo.node.x = dragInfo.origX + (x - dragInfo.startX) / vs.zoom;
          dragInfo.node.y = dragInfo.origY + (y - dragInfo.startY) / vs.zoom;
          redraw();
          return;
        }
      }

      if (panInfo) {
        vs.panX = panInfo.startPanX + (x - panInfo.startX);
        vs.panY = panInfo.startPanY + (y - panInfo.startY);
        redraw();
        return;
      }

      // hover
      const node = findNodeAt(x, y, vs);
      if (node !== vs.hoveredNode) {
        vs.hoveredNode = node;
        canvas.style.cursor = node ? 'pointer' : 'default';
        redraw();
      }
    });

    canvas.addEventListener('mousedown', (e) => {
      const { x, y } = screenOffset(e);

      if (e.button === 0) {
        const node = findNodeAt(x, y, vs);
        if (node) {
          dragInfo = {
            node,
            startX: x, startY: y,
            origX: node.x, origY: node.y,
            dragging: false,
          };
          vs.selectedNode = node;
          redraw();
          return;
        }
        // left button on empty space — pan
        panInfo = { startX: x, startY: y, startPanX: vs.panX, startPanY: vs.panY };
        canvas.style.cursor = 'grabbing';
        return;
      }
    });

    window.addEventListener('mouseup', () => {
      if (dragInfo) {
        if (!dragInfo.dragging && callbacks.onNodeClick) {
          callbacks.onNodeClick(dragInfo.node);
        }
        dragInfo = null;
        canvas.style.cursor = vs.hoveredNode ? 'pointer' : 'default';
      }
      if (panInfo) {
        panInfo = null;
        canvas.style.cursor = vs.hoveredNode ? 'pointer' : 'default';
      }
    });

    canvas.addEventListener('wheel', (e) => {
      e.preventDefault();
      const { x: mx, y: my } = screenOffset(e);
      const oldZoom = vs.zoom;
      const delta = e.deltaY > 0 ? 0.9 : 1 / 0.9;
      const newZoom = Math.min(2.0, Math.max(0.3, oldZoom * delta));

      // zoom centered on mouse position
      const wx = (mx - vs.panX) / oldZoom;
      const wy = (my - vs.panY) / oldZoom;
      vs.zoom = newZoom;
      vs.panX = mx - wx * newZoom;
      vs.panY = my - wy * newZoom;

      redraw();
    }, { passive: false });

    canvas.addEventListener('contextmenu', (e) => {
      e.preventDefault();
      const { x, y } = screenOffset(e);
      const node = findNodeAt(x, y, vs);
      if (node) {
        vs.selectedNode = node;
        redraw();
        _showCtxMenu(e, node, callbacks);
      }
    });

    document.addEventListener('click', (e) => {
      // hide context menu when clicking outside
      if (_ctxMenu && !_ctxMenu.contains(e.target)) {
        _hideCtxMenu();
      }
    });

    return { redraw };
  }

  // ── public API ────────────────────────────────────────────────────────
  return {
    NODE_WIDTH,
    NODE_HEIGHT,
    createViewState,
    screenToWorld,
    worldToScreen,
    layoutNodes,
    drawMindmap,
    findNodeAt,
    attachEvents,
  };
})();
