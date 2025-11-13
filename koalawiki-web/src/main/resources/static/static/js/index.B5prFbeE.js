import{c as F,B as w,_ as V}from"./index.DVxJNWKz.js";import{j as e}from"./ui-vendor.qRFmETy1.js";import{d as A,f as W,r as u}from"./react-vendor.DvRQROtf.js";import{u as G}from"./repositoryDetail.store.C-Ry8mIm.js";import{u as q,f as J}from"./fetch.DzjyCgvm.js";import{C as K,a as Q,d as Z}from"./card.BWSfnS8k.js";import{T as ee,a as k,b as N,c as L}from"./tooltip.DTPyC1Lx.js";import{S as te}from"./skeleton.D12YPcXk.js";import{C as ne}from"./circle-alert.CHQhJqf-.js";import{R as P}from"./refresh-cw.J9q3vuUY.js";import{D as re}from"./download.CeaF0tEZ.js";import"./warehouse.service.Bp5qDeEn.js";import"./react.DPTj6gG5.js";const oe=[["path",{d:"M8 3H5a2 2 0 0 0-2 2v3",key:"1dcmit"}],["path",{d:"M21 8V5a2 2 0 0 0-2-2h-3",key:"1e4gt3"}],["path",{d:"M3 16v3a2 2 0 0 0 2 2h3",key:"wsl5sc"}],["path",{d:"M16 21h3a2 2 0 0 0 2-2v-3",key:"18trek"}]],ie=F("maximize",oe);const ae=[["path",{d:"M8 3v3a2 2 0 0 1-2 2H3",key:"hohbtr"}],["path",{d:"M21 8h-3a2 2 0 0 1-2-2V3",key:"5jw1f3"}],["path",{d:"M3 16h3a2 2 0 0 1 2 2v3",key:"198tvr"}],["path",{d:"M16 21v-3a2 2 0 0 1 2-2h3",key:"ph8mxp"}]],se=F("minimize",ae);class ce{async getMindMap(d,a,s,o){try{const i=new URLSearchParams({owner:d,name:a});return s&&i.append("branch",s),o&&i.append("languageCode",o),await J.get(`/api/Warehouse/MiniMap?${i.toString()}`)}catch(i){throw console.error("Failed to fetch mind map:",i),i}}}const le=new ce,de=v=>{let d=0;const a=(s,o=!1)=>{const i={topic:s.title,id:o?"root":`node_${d++}`,hyperLink:s.url};return o&&(i.root=!0),s.nodes&&s.nodes.length>0&&(i.expanded=!0,i.children=s.nodes.map(b=>a(b))),i};return a(v,!0)};function Ne({className:v}){const{owner:d,name:a}=A(),[s]=W(),{t:o,i18n:i}=q(),{selectedBranch:b}=G(),[U,E]=u.useState(!0),[me,$]=u.useState(null),[S,C]=u.useState(""),[h,I]=u.useState(!1),x=u.useRef(null),m=u.useRef(null),c=u.useRef(null),y=s.get("branch")||b||"main",M=async()=>{if(!(!d||!a)){E(!0);try{const t=await le.getMindMap(d,a,y,i.language);if(t.code===200&&t.data){$(t.data);const p=de(t.data);setTimeout(()=>X(p),100)}else C(t.message||o("repository.mindMap.loadFailed"))}catch(t){console.error("Failed to fetch mind map:",t),C(t?.message||o("repository.mindMap.loadFailed"))}finally{E(!1)}}},X=async t=>{if(!x.current)return;const p=(await V(async()=>{const{default:r}=await import("./MindElixir.Dldtst4e.js");return{default:r}},[])).default;c.current&&(c.current(),c.current=null),m.current&&m.current.destroy?.();const f={el:x.current,direction:p.SIDE,draggable:!0,contextMenu:!0,toolBar:!1,nodeMenu:!0,keypress:!0,locale:"en",overflowHidden:!1,mainLinkStyle:2,mouseSelectionButton:0,allowFreeTransform:!0,mouseMoveThreshold:5,primaryLinkStyle:1,primaryNodeHorizontalGap:65,primaryNodeVerticalGap:25,theme:{name:"Minimal",palette:["#0f172a","#475569","#64748b","#94a3b8","#cbd5e1","#e2e8f0","#f1f5f9","#f8fafc","#0ea5e9","#06b6d4"],cssVar:{"--main-color":"#0f172a","--main-bgcolor":"#2f2020","--color":"#1e293b","--bgcolor":"#f8fafc","--panel-color":"255, 255, 255","--panel-bgcolor":"248, 250, 252"}}},n=new p(f),H={nodeData:t,linkData:{}};n.init(H);const j=()=>{n.scaleFit(),n.toCenter()};typeof window<"u"?(requestAnimationFrame(j),setTimeout(j,150)):j();const l={isPanning:!1,lastX:0,lastY:0},O=r=>r instanceof HTMLElement?!!(r.closest("me-root")||r.closest("me-parent")||r.closest("me-tpc")||r.closest("#input-box")):!1,D=r=>{r.button===0&&(O(r.target)||(l.isPanning=!0,l.lastX=r.clientX,l.lastY=r.clientY,n.container&&(n.container.style.cursor="grabbing",n.container.classList.add("grabbing")),r.preventDefault()))},_=r=>{if(!l.isPanning)return;const z=r.clientX-l.lastX,R=r.clientY-l.lastY;(z!==0||R!==0)&&(n.move(z,R),l.lastX=r.clientX,l.lastY=r.clientY)},g=()=>{l.isPanning&&(l.isPanning=!1,n.container&&(n.container.style.cursor="grab",n.container.classList.remove("grabbing")))};n.container?.addEventListener("mousedown",D),window.addEventListener("mousemove",_),window.addEventListener("mouseup",g),n.container?.addEventListener("mouseleave",g),c.current=()=>{n.container?.removeEventListener("mousedown",D),window.removeEventListener("mousemove",_),window.removeEventListener("mouseup",g),n.container?.removeEventListener("mouseleave",g),n.container&&(n.container.style.cursor="",n.container.classList.remove("grabbing"))},n.container&&(n.container.style.cursor="grab",n.container.classList.remove("grabbing")),n.bus.addListener("selectNode",r=>{r.hyperLink&&window.open(r.hyperLink,"_blank")}),n.bus.addListener("operation",r=>{console.log("Mind map operation:",r)}),m.current=n;const T=new ResizeObserver(()=>{n&&x.current&&n.refresh()});return T.observe(x.current),()=>{T.disconnect(),c.current&&(c.current(),c.current=null),n&&n.destroy?.()}},Y=()=>{I(!h),setTimeout(()=>{m.current&&x.current&&m.current.refresh()},100)},B=async()=>{if(!m.current){console.error("思维导图未初始化");return}try{const t=await m.current.exportPng();if(t){const p=URL.createObjectURL(t),f=document.createElement("a");f.href=p,f.download=`${d}-${a}-mindmap.png`,document.body.appendChild(f),f.click(),document.body.removeChild(f),URL.revokeObjectURL(p)}}catch(t){console.error("Export error:",t)}};return u.useEffect(()=>{M()},[d,a,y,i.language]),u.useEffect(()=>()=>{c.current&&(c.current(),c.current=null),m.current&&m.current.destroy?.()},[]),U?e.jsx("div",{className:"flex justify-center items-center h-[60vh]",children:e.jsx(te,{className:"w-32 h-32"})}):S?e.jsxs("div",{className:"flex flex-col items-center justify-center h-full gap-4",children:[e.jsx(ne,{className:"h-12 w-12 text-destructive"}),e.jsxs("div",{className:"text-center space-y-2",children:[e.jsx("p",{className:"text-lg font-medium",children:o("repository.mindMap.error")}),e.jsx("p",{className:"text-sm text-muted-foreground",children:S}),e.jsxs("p",{className:"text-xs text-muted-foreground",children:[d,"/",a," - ",y]})]}),e.jsxs(w,{onClick:M,variant:"outline",className:"gap-2",children:[e.jsx(P,{className:"h-4 w-4"}),o("common.retry")]})]}):e.jsx(ee,{children:e.jsxs("div",{className:"h-full",children:[e.jsxs(K,{className:`
          relative flex flex-col
          ${h?"h-screen fixed top-0 left-0 w-screen z-[9999]":"h-[85vh]"}
          transition-all duration-300 border-border/50 shadow-sm
        `,children:[e.jsx(Q,{children:e.jsx("div",{className:"flex justify-between  sm:flex-nowrap",children:e.jsxs("div",{className:"flex justify-end",children:[e.jsxs(k,{children:[e.jsx(N,{asChild:!0,children:e.jsx(w,{variant:"ghost",size:"icon",onClick:M,className:"h-8 w-8",children:e.jsx(P,{className:"h-4 w-4"})})}),e.jsx(L,{children:e.jsx("p",{children:o("repository.mindMap.refresh")})})]}),e.jsxs(k,{children:[e.jsx(N,{asChild:!0,children:e.jsx(w,{variant:"ghost",size:"icon",onClick:B,className:"h-8 w-8",children:e.jsx(re,{className:"h-4 w-4"})})}),e.jsx(L,{children:e.jsx("p",{children:o("repository.mindMap.exportImage")})})]}),e.jsxs(k,{children:[e.jsx(N,{asChild:!0,children:e.jsx(w,{variant:"ghost",size:"icon",onClick:Y,className:"h-8 w-8",children:h?e.jsx(se,{className:"h-4 w-4"}):e.jsx(ie,{className:"h-4 w-4"})})}),e.jsx(L,{children:e.jsx("p",{children:o(h?"repository.mindMap.fullscreenExit":"repository.mindMap.fullscreenEnter")})})]})]})})}),e.jsxs(Z,{className:`
          ${h?"h-[calc(100vh-80px)]":"h-[calc(85vh-80px)]"}
          p-0 relative
          w-full
        `,children:[e.jsx("div",{ref:x,className:`
              w-full h-full relative select-none
              bg-gradient-to-br from-slate-50 to-slate-200
              ${h?"rounded-none":"rounded-lg"}
            `,style:{touchAction:"none",WebkitUserSelect:"none"},onContextMenu:t=>{t.preventDefault()},onMouseDown:t=>{t.button===2&&t.preventDefault()},onTouchStart:t=>{t.touches.length>1&&t.preventDefault()},onTouchMove:t=>{t.preventDefault()},onDragStart:t=>{t.preventDefault()}}),e.jsx("div",{className:"absolute bottom-4 right-4 bg-black/70 text-white px-3 py-2 rounded text-xs z-[1000]",children:o("repository.mindMap.helpText")})]})]}),e.jsx("style",{jsx:!0,global:!0,children:`
        .mind-elixir {
          width: 100%;
          height: 100%;
          touch-action: none !important;
          user-select: none !important;
          WebkitUserSelect: none !important;
          MozUserSelect: none !important;
          MsUserSelect: none !important;
        }

        .mind-elixir .map-container {
          background: transparent !important;
          touch-action: none !important;
          -ms-touch-action: none !important;
          -webkit-touch-callout: none !important;
          cursor: grab;
        }

        .mind-elixir .map-container.is-panning,
        .mind-elixir .map-container.grabbing {
          cursor: grabbing !important;
        }

        .mind-elixir .node-container {
          cursor: pointer;
          touch-action: none !important;
        }

        .mind-elixir .node-container:hover {
          opacity: 0.9;
          transform: scale(1.02);
          transition: all 0.2s ease-in-out;
        }

        .mind-elixir .line {
          stroke: #475569;
          stroke-width: 1.5;
        }

        .mind-elixir .node {
          border-radius: 8px;
          border: 1px solid #e2e8f0;
          background: #ffffff;
          box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
          touch-action: none !important;
          font-family: inherit;
        }

        .mind-elixir .root {
          background: linear-gradient(135deg, #0f172a, #1e293b) !important;
          color: white !important;
          font-weight: 600;
          font-size: 16px;
          border: none !important;
          box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1) !important;
        }

        .mind-elixir .primary {
          background: #f8fafc !important;
          border-color: #cbd5e1 !important;
          color: #334155 !important;
          font-weight: 500;
        }

        .mind-elixir .context-menu {
          border-radius: 8px;
          box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
          border: 1px solid #e2e8f0;
          z-index: 9999 !important;
          background: white;
        }

        html {
          -ms-touch-action: none !important;
          touch-action: none !important;
        }

        body {
          -ms-touch-action: manipulation !important;
          touch-action: manipulation !important;
        }

        * {
          -webkit-touch-callout: none !important;
          -webkit-user-select: none !important;
          -khtml-user-select: none !important;
          -moz-user-select: none !important;
          -ms-user-select: none !important;
        }

        input, textarea, [contenteditable] {
          -webkit-user-select: text !important;
          -moz-user-select: text !important;
          -ms-user-select: text !important;
          user-select: text !important;
        }

        .mind-elixir ::-webkit-scrollbar {
          width: 6px;
          height: 6px;
        }

        .mind-elixir ::-webkit-scrollbar-track {
          background: #f1f5f9;
          border-radius: 3px;
        }

        .mind-elixir ::-webkit-scrollbar-thumb {
          background: #cbd5e1;
          border-radius: 3px;
        }

        .mind-elixir ::-webkit-scrollbar-thumb:hover {
          background: #94a3b8;
        }
      `})]})})}export{Ne as default};
