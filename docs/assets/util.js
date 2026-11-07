const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const safeUrl = u => /^https?:\/\//i.test(u || '') ? u : '';
const $ = (s, r = document) => r.querySelector(s);
const configured = () => !/YOUR_/.test(GS.SUPABASE_URL + GS.SUPABASE_KEY);
function snack(msg, ms = 4500) {
  let e = $('.snack');
  if (!e) { e = document.createElement('div'); e.className = 'snack'; document.body.append(e); }
  e.textContent = msg; e.classList.add('show');
  clearTimeout(e._t); e._t = setTimeout(() => e.classList.remove('show'), ms);
}
