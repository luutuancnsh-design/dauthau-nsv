/* Trang chi tiết gói thầu (dùng chung cho web và app).
   Dữ liệu: data/ct/<mã>.json do trạm thu lấy từ muasamcong. Bấm mã IB/RQ mới chuyển sang muasamcong. */
(function () {
  const CSS = `
.ctbg{position:fixed;inset:0;background:rgba(0,0,0,.45);z-index:80;display:flex;align-items:flex-start;justify-content:center;padding:24px 16px;overflow:auto}
.ct{background:var(--panel,#fff);color:var(--fg,#14211f);border-radius:16px;width:min(1000px,100%);box-shadow:0 20px 60px rgba(0,0,0,.35);overflow:hidden;font-size:.9rem}
.ct-h{background:linear-gradient(135deg,var(--hero1,#0b3d3a),var(--hero2,#0f766e));color:#fff;padding:16px 18px;position:sticky;top:0;z-index:2}
.ct-h .r1{display:flex;gap:8px;align-items:center;flex-wrap:wrap}
.ct-h .bdg{background:rgba(255,255,255,.18);border-radius:6px;padding:1px 8px;font-size:.72rem;font-weight:700}
.ct-h .bdg.red{background:#e11d48}
.ct-h a.no{color:#fff;font-weight:700;text-decoration:underline;text-underline-offset:3px;font-family:ui-monospace,Menlo,Consolas,monospace}
.ct-h .x{margin-left:auto;border:0;background:rgba(255,255,255,.16);color:#fff;border-radius:10px;padding:6px 12px;font-weight:600;cursor:pointer;font:inherit}
.ct-h h3{margin:8px 0 2px;font-size:1.08rem;line-height:1.35}
.ct-h .ct-inv{color:rgba(255,255,255,.88);font-size:.85rem}
.ct-b{padding:6px 18px 20px}
.ct-sec{margin-top:16px}
.ct-sec>h4{margin:0 0 8px;font-size:.78rem;text-transform:uppercase;letter-spacing:.04em;color:var(--muted,#5d6b68)}
.ct-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(210px,1fr));gap:8px}
@media (max-width:720px){.ct-grid{grid-template-columns:1fr 1fr}}
.ct-kv{background:var(--panel2,#f8faf9);border:1px solid var(--line2,#eef2f1);border-radius:10px;padding:8px 10px}
.ct-kv span{display:block;font-size:.72rem;color:var(--muted,#5d6b68)}.ct-kv b{font-weight:600;word-break:break-word}
.ct-files{display:flex;flex-wrap:wrap;gap:8px}
.ct-file{display:inline-flex;align-items:center;gap:6px;border:1px solid var(--line,#e2e8e6);border-radius:10px;padding:7px 11px;color:var(--accent,#0f766e);font-weight:600;text-decoration:none;font-size:.84rem;background:var(--panel,#fff);cursor:pointer}
.ct-file svg{width:15px;height:15px;flex:none}
.ct-tw{overflow-x:auto;border:1px solid var(--line2,#eef2f1);border-radius:12px}
table.ctt{width:100%;border-collapse:collapse;font-size:.82rem}
table.ctt th{text-align:left;font-size:.7rem;text-transform:uppercase;letter-spacing:.03em;color:var(--muted,#5d6b68);padding:8px;background:var(--panel2,#f8faf9);border-bottom:1px solid var(--line,#e2e8e6);white-space:nowrap}
table.ctt td{padding:8px;border-bottom:1px solid var(--line2,#eef2f1);vertical-align:top}
table.ctt td.n{text-align:right;white-space:nowrap}
.ct-lot{display:block;font-size:.72rem;color:var(--muted,#5d6b68)}
.ct-spec{margin-top:4px}.ct-spec summary{cursor:pointer;color:var(--accent,#0f766e);font-size:.76rem;font-weight:600}
.ct-spec pre{white-space:pre-wrap;font:inherit;font-size:.78rem;background:var(--panel2,#f8faf9);border-radius:8px;padding:8px;margin:6px 0 0;max-height:320px;overflow:auto}
.ct-win{color:#15803d;font-weight:700}.ct-lose{color:#be123c;font-weight:700}
.ct-note{font-size:.8rem;color:var(--muted,#5d6b68);margin-top:6px}
.ct-warn{background:#fde8ec;color:#9f1239;border-radius:10px;padding:10px 12px;font-weight:600}
.ct-info{background:var(--accentbg,#e6f4f2);border-radius:10px;padding:10px 12px;font-size:.85rem}
.ct-go{display:inline-flex;align-items:center;gap:6px;border:0;border-radius:11px;padding:9px 14px;background:var(--accent,#0f766e);color:#fff;font-weight:600;text-decoration:none;font-size:.86rem}
@media (max-width:720px){.ctbg{padding:0}.ct{border-radius:0;min-height:100%}.ct-b{padding:4px 14px 20px}table.ctt{font-size:.78rem}}
@media (prefers-color-scheme:dark){.ct-warn{background:#3b1a22;color:#fecdd3}.ct-win{color:#7ddc9c}.ct-lose{color:#fb7185}}`;
  const st = document.createElement('style'); st.textContent = CSS; document.head.append(st);

  const HT = { DTRR: 'Đấu thầu rộng rãi', DTHC: 'Đấu thầu hạn chế', CHCT: 'Chào hàng cạnh tranh', CHCTRG: 'Chào hàng cạnh tranh rút gọn', CGTTRG: 'Chào giá trực tuyến rút gọn', CDTRG: 'Chỉ định thầu rút gọn', CDT: 'Chỉ định thầu', MSTT: 'Mua sắm trực tiếp', TTH: 'Tự thực hiện', HHDVK: 'Hàng hóa, dịch vụ khác', THUOC: 'Thuốc' };
  const HD = { DGCD: 'Đơn giá cố định', DGDC: 'Đơn giá điều chỉnh', TG: 'Trọn gói', TLPT: 'Theo tỷ lệ phần trăm', TGTT: 'Theo thời gian' };
  const DL = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><path d="M7 10l5 5 5-5"/><path d="M12 15V3"/></svg>';
  const E = (t, c, x) => { const e = document.createElement(t); if (c) e.className = c; if (x !== undefined && x !== null) e.textContent = x; return e; };
  const N = (v) => (v === null || v === undefined || v === '' || Number(v) === 0) ? '' : Number(v).toLocaleString('vi-VN');
  const T = (s) => { const m = /^(\d{4})-(\d\d)-(\d\d)T(\d\d):(\d\d)/.exec(s || ''); return m ? m[4] + ':' + m[5] + ' ' + m[3] + '/' + m[2] + '/' + m[1] : (s || ''); };
  const P = 'egpportalcontractorselectionv2_WAR_egpportalcontractorselectionv2';
  function msc(no, d) {
    const id = (d && d.id) || no, ir = (d && d.ir) || '', yc = (d && d.loai === 'ycbg') || /^RQ/.test(no);
    const q = yc ? { p_p_id: P, p_p_lifecycle: '0', p_p_state: 'normal', p_p_mode: 'view', ['_' + P + '_render']: 'detail-v2', type: 'es-ycbg', stepCode: 'request-quote-step-1', id, notifyNo: no, isInternet: '1' }
      : { p_p_id: P, p_p_lifecycle: '0', p_p_state: 'normal', p_p_mode: 'view', ['_' + P + '_render']: 'detail-v2', type: 'es-notify-contractor', stepCode: ir ? 'notify-contractor-step-4-kqlcnt' : 'notify-contractor-step-1-tbmt', step: 'tbmt', id, notifyId: id, inputResultId: ir || 'undefined', bidInviteId: 'undefined', notifyNo: no, planNo: 'undefined', isInternet: '1', processApply: 'LDT', bidMode: '1_MTHS', bidForm: (d && d.hinhthuc) || '' };
    return 'https://muasamcong.mof.gov.vn/web/guest/contractor-selection?' + new URLSearchParams(q);
  }
  function openExt(u) { const A = window.DauThauApp || (window.parent !== window && window.parent.DauThauApp); if (A && A.openUrl) A.openUrl(u); else window.open(u, '_blank', 'noopener'); }
  function fileLink(f, label) {
    const a = E('a', 'ct-file'); a.href = 'https://muasamcong.mof.gov.vn/api/unau/edocproxy/file/share/' + encodeURIComponent(f.id); a.setAttribute('download', ''); a.rel = 'noopener';
    a.innerHTML = DL; a.append(label + (f.ten ? ': ' + f.ten : ''));
    const A = window.DauThauApp || (window.parent !== window && window.parent.DauThauApp);
    if (A && A.download) a.addEventListener('click', (e) => { e.preventDefault(); A.download(a.href, f.ten || label); });
    return a;
  }
  function kv(grid, k, v) { if (!v) return; const d = E('div', 'ct-kv'); d.append(E('span', null, k), E('b', null, v)); grid.append(d); }
  function sec(title) { const s = E('div', 'ct-sec'); s.append(E('h4', null, title)); return s; }
  function specCell(td, spec) { if (!spec) return; const d = E('details', 'ct-spec'); d.append(E('summary', null, 'Xem yêu cầu kỹ thuật'), E('pre', null, spec)); td.append(d); }

  function render(box, no, d, h) {
    d = d || {}; h = h || {};
    const yc = d.loai === 'ycbg' || /^RQ/.test(no);
    const ten = (yc && d.ten_yc) ? d.ten_yc : (d.ten || h.ten || '');
    const head = E('div', 'ct-h'), r1 = E('div', 'r1');
    r1.append(E('span', 'bdg', yc ? 'Yêu cầu báo giá' : (d.kq ? 'Đã có KQLCNT' : 'Thông báo mời thầu')));
    if (d.kq && d.kq.huy) r1.append(E('span', 'bdg red', 'Hủy thầu'));
    const a = E('a', 'no', no); a.href = msc(no, d); a.target = '_blank'; a.rel = 'noopener'; a.title = 'Mở trên muasamcong';
    a.addEventListener('click', (e) => { e.preventDefault(); openExt(a.href); });
    r1.append(a);
    const x = E('button', 'x', 'Đóng'); x.type = 'button'; r1.append(x); head.append(r1, E('h3', null, ten), E('div', 'ct-inv', d.bmt || h.bmt || ''));
    const body = E('div', 'ct-b');

    if (!d.no) body.append(E('div', 'ct-info', 'Chưa có chi tiết của gói này trong dữ liệu (trạm thu chỉ lấy hoạt động của đơn vị y tế trong 3 tuần gần đây). Bấm mã ' + no + ' ở trên để xem trên muasamcong.'));
    const g = E('div', 'ct-grid');
    const hf = d.hinhthuc || h.hinhthuc || ''; kv(g, yc ? 'Loại báo giá' : 'Hình thức', HT[hf] || hf);
    if (yc && d.ten && d.ten !== ten) kv(g, 'Dự toán / gói thầu', d.ten);
    kv(g, yc ? 'Giá dự kiến' : 'Giá gói thầu', N(d.gia || h.gia) ? N(d.gia || h.gia) + ' đ' : '');
    kv(g, 'Thời gian thực hiện', d.tg || h.tg);
    kv(g, 'Ngày đăng tải', T(d.dang || h.dang));
    kv(g, yc ? 'Hạn nhận báo giá' : 'Thời điểm đóng thầu', T(d.dong || h.dong));
    kv(g, 'Thời điểm mở thầu', T(d.mo));
    kv(g, 'Loại hợp đồng', HD[d.loaihd] || d.loaihd);
    kv(g, 'Nguồn vốn', d.nguonvon);
    kv(g, 'Bảo đảm dự thầu', N(d.baodam) ? N(d.baodam) + ' đ' : '');
    kv(g, yc ? 'Hiệu lực báo giá' : 'Hiệu lực HSDT', d.hieuluc);
    if (g.children.length) { const s = sec('Thông tin chung'); s.append(g); body.append(s); }

    const files = E('div', 'ct-files');
    if (d.file && d.file.id) files.append(fileLink(d.file, yc ? 'File yêu cầu báo giá' : 'Chương V'));
    else if (h.ch5 && h.ch5.id) files.append(fileLink({ id: h.ch5.id, ten: h.ch5.name }, 'Chương V'));
    if (d.qd && d.qd.id) files.append(fileLink(d.qd, 'QĐ phê duyệt HSMT'));
    if (d.kq && d.kq.qd && d.kq.qd.id) files.append(fileLink(d.kq.qd, 'QĐ phê duyệt KQLCNT'));
    if (d.kq && d.kq.qd_huy && d.kq.qd_huy.id) files.append(fileLink(d.kq.qd_huy, 'QĐ hủy thầu'));
    if (files.children.length) { const s = sec('Hồ sơ đính kèm'); s.append(files); body.append(s); }

    if (window.D && window.D.match && window.D.match[no] && window.mtChips) { const s = sec('Đối chiếu thiết bị đang cung cấp'); const b = E('div'); window.mtChips(b, { notifyNo: no, investor: d.bmt || h.bmt }); s.append(b); body.append(s); }

    const rows = d.hang || [];
    if (rows.length) {
      const s = sec('Danh mục hàng hóa (' + rows.length + (rows.length >= 200 ? '+' : '') + ' mặt hàng)');
      const tw = E('div', 'ct-tw'), t = E('table', 'ctt'), th = E('thead'), tb = E('tbody');
      const hasMk = rows.some((r) => r[3] || r[4] || r[5]), hasP = rows.some((r) => Number(r[6]) > 0);
      th.innerHTML = '<tr><th>#</th><th>Tên hàng hóa</th><th class="n">SL</th><th>ĐVT</th>' + (hasMk ? '<th>Ký mã hiệu / Hãng / Xuất xứ</th>' : '') + (hasP ? '<th class="n">Giá phần/lô</th>' : '') + '</tr>';
      rows.forEach((r, i) => {
        const tr = E('tr'); tr.append(E('td', null, String(i + 1)));
        const n = E('td'); n.append(E('div', null, r[0])); if (r[8]) n.append(E('span', 'ct-lot', r[8])); specCell(n, r[7]); tr.append(n);
        tr.append(E('td', 'n', N(r[1]) || r[1]), E('td', null, r[2]));
        if (hasMk) tr.append(E('td', null, [r[3], r[4], r[5]].filter(Boolean).join(' · ')));
        if (hasP) tr.append(E('td', 'n', N(r[6])));
        tb.append(tr);
      });
      t.append(th, tb); tw.append(t); s.append(tw); body.append(s);
    } else if (h.goods && h.goods.length) {
      const s = sec('Hàng hóa trúng thầu'); const tw = E('div', 'ct-tw'), t = E('table', 'ctt');
      t.innerHTML = '<thead><tr><th>Hàng hóa</th><th>Model / KMH</th><th>Hãng / Xuất xứ</th><th class="n">SL</th><th class="n">Đơn giá</th><th>Nhà thầu</th></tr></thead>';
      const tb = E('tbody'); h.goods.forEach((r) => { const tr = E('tr'); tr.append(E('td', null, r.item), E('td', null, [r.model, r.kmh].filter(Boolean).join(' / ')), E('td', null, [r.hang, r.xuatxu].filter(Boolean).join(' · ')), E('td', 'n', N(r.sl)), E('td', 'n', N(r.dongia)), E('td', null, r.winner || '')); tb.append(tr); });
      t.append(tb); tw.append(t); s.append(tw); body.append(s);
    }

    if (d.kq) {
      const k = d.kq, s = sec('Kết quả lựa chọn nhà thầu' + (k.ngay ? ' · đăng ' + T(k.ngay) : ''));
      if (k.huy) s.append(E('div', 'ct-warn', 'Gói thầu bị hủy' + (k.lydo ? ' (mã lý do: ' + k.lydo + ')' : '') + '. Xem quyết định hủy ở phần hồ sơ đính kèm.'));
      if (k.mo) s.append(E('div', 'ct-note', 'Mở thầu: ' + T(k.mo)));
      if (k.lo && k.lo.length) {
        const tw = E('div', 'ct-tw'), t = E('table', 'ctt'); t.style.marginTop = '8px';
        const multi = new Set(k.lo.map((r) => r[0])).size > 1;
        t.innerHTML = '<thead><tr>' + (multi ? '<th>Phần/lô</th>' : '') + '<th>Nhà thầu tham dự</th><th class="n">Giá dự thầu</th><th class="n">Giá trúng/đánh giá</th><th>Kết quả</th></tr></thead>';
        const tb = E('tbody');
        k.lo.slice().sort((a, b) => (b[4] - a[4])).forEach((r) => { const tr = E('tr'); if (multi) tr.append(E('td', null, r[0])); tr.append(E('td', null, r[1]), E('td', 'n', N(r[2])), E('td', 'n', N(r[3])));
          const c = E('td'); c.append(E('span', r[4] ? 'ct-win' : 'ct-lose', r[4] ? 'Trúng thầu' : 'Không trúng')); if (!r[4] && r[5]) c.append(E('div', 'ct-note', r[5])); tr.append(c); tb.append(tr); });
        t.append(tb); tw.append(t); s.append(tw);
      }
      if (k.hang && k.hang.length) {
        s.append(E('div', 'ct-note', 'Hàng hóa trúng thầu (' + k.hang.length + ' mặt hàng):'));
        const tw = E('div', 'ct-tw'), t = E('table', 'ctt'); t.style.marginTop = '6px';
        t.innerHTML = '<thead><tr><th>Hàng hóa</th><th>Ký mã hiệu / Hãng / Xuất xứ</th><th class="n">SL</th><th>ĐVT</th><th class="n">Đơn giá</th><th>Nhà thầu</th></tr></thead>';
        const tb = E('tbody'); k.hang.forEach((r) => { const tr = E('tr'); const n = E('td', null, r[0]); if (r[8]) n.append(E('span', 'ct-lot', r[8])); tr.append(n, E('td', null, [r[3], r[4], r[5]].filter(Boolean).join(' · ')), E('td', 'n', N(r[1]) || r[1]), E('td', null, r[2]), E('td', 'n', N(r[6])), E('td', null, r[9] || '')); tb.append(tr); });
        t.append(tb); tw.append(t); s.append(tw);
      }
      body.append(s);
    }

    const go = E('div', 'ct-sec'); const b = E('a', 'ct-go', 'Mở gói ' + no + ' trên muasamcong'); b.href = msc(no, d); b.target = '_blank'; b.rel = 'noopener';
    b.addEventListener('click', (e) => { e.preventDefault(); openExt(b.href); });
    go.append(b); if (d.u) go.append(E('div', 'ct-note', 'Dữ liệu lấy từ muasamcong lúc ' + T(d.u) + '.')); body.append(go);
    box.replaceChildren(head, body);
    return x;
  }

  window.CT = {
    async open(no, hint) {
      const bg = E('div', 'ctbg'), box = E('div', 'ct'); bg.append(box); document.body.append(bg);
      const prevOverflow = document.body.style.overflow; document.body.style.overflow = 'hidden';
      const close = () => { bg.remove(); document.body.style.overflow = prevOverflow; document.removeEventListener('keydown', esc); };
      const esc = (e) => { if (e.key === 'Escape') close(); };
      document.addEventListener('keydown', esc); bg.addEventListener('click', (e) => { if (e.target === bg) close(); });
      render(box, no, null, hint).addEventListener('click', close);
      let d = null;
      try { const r = await fetch('data/ct/' + encodeURIComponent(no) + '.json?v=' + Date.now()); if (r.ok) d = await r.json(); } catch (e) { }
      if (d) render(box, no, d, hint).addEventListener('click', close);
      else { const h = box.querySelector('.ct-b'); }
    },
    url: msc
  };
})();
