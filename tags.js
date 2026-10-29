/* Gắn nhãn hãng/model từ tên gói, tên lô và danh mục hàng. Dùng chung cho web và task cập nhật. */
(function(g){
const R=(s)=>new RegExp(s,'i');
const C='cell?[\\s-]*tac[\\s-]*';
const LIST=[
 ['Celltac G',R(C+'g(?![a-z])')],['Celltac Alpha (α)',R(C+'(alpha|α)')],['Celltac Es',R(C+'es(?![a-z])')],
 ['Celltac E',R(C+'e(?![a-z])')],['Celltac F',R(C+'f(?![a-z])')],
 ['Compact X',R('compact[\\s-]*x(?![a-z])')],['Thrombolyzer XRC',R('thrombolyzer[\\s-]*xr[\\s-]*c')],['Thrombolyzer XRM',R('thrombolyzer[\\s-]*xr[\\s-]*m')],
 ['IH-500',R('(?<![a-z0-9])ih[\\s-]*500(?!\\d)')],['IH-100',R('(?<![a-z0-9])ih[\\s-]*100(?!\\d)')],['IH-1000',R('(?<![a-z0-9])ih[\\s-]*1000(?!\\d)')],
 ['D-10',R('(?<![a-z0-9.\\/-])d-10(?![\\d.,])')],['D-100',R('(?<![a-z0-9.\\/-])d-100(?![\\d.,])')],
 ['Mixrate',R('mix[\\s-]*rate')],['Microsed',R('micro[\\s-]*sed(?![a-z])')],
 ['490-2D',R('490[\\s-]*2d(?![a-z])')],['490 4+',R('490[\\s-]*4\\s*\\+')],
 ['CA-270',R('(?<![a-z0-9])ca(\\s*-\\s*|)270(?!\\d)')],['CA-400',R('(?<![a-z0-9])ca(\\s*-\\s*|)400(?!\\d)')],['CA-800',R('(?<![a-z0-9])ca(\\s*-\\s*|)800(?!\\d)')],['CA-310',R('(?<![a-z0-9])ca(\\s*-\\s*|)310(?!\\d)')],
 ['EXIAS',R('exias')],
 ['Urilyzer 100',R('urilyzer[\\s-]*100(?!\\d)')],['Urilyzer Auto',R('urilyzer[\\s-]*auto')],['Urilyzer 500',R('urilyzer[\\s-]*500(?!\\d)')],['Urilyzer Cell',R('urilyzer[\\s-]*cell')],
 ['Nihon Kohden',R('nihon[\\s-]*kohden')],['Furuno',R('furuno')],['Analyticon',R('analyticon')]
];
const MEK={'6400':'Celltac Alpha (α)','6410':'Celltac Alpha (α)','6420':'Celltac Alpha (α)','6500':'Celltac Alpha (α)','6510':'Celltac Alpha (α)','6550':'Celltac Alpha (α)','7222':'Celltac E','7300':'Celltac Es','8222':'Celltac F','9100':'Celltac G'};
/* Nhãn hãng tự thêm khi thấy model của hãng */
const BRAND={'Nihon Kohden':/^(Celltac|MEK-)/,'Furuno':/^CA-/,'Analyticon':/^Urilyzer/};
const ORDER=['Nihon Kohden','Celltac','Celltac G','Celltac Alpha (α)','Celltac Es','Celltac E','Celltac F','MEK-','Furuno','CA-270','CA-310','CA-400','CA-800','Analyticon','Urilyzer','Urilyzer 100','Urilyzer 500','Urilyzer Auto','Urilyzer Cell','EXIAS','Thrombolyzer','Thrombolyzer XRC','Thrombolyzer XRM','Compact X','IH-100','IH-500','IH-1000','D-10','D-100','Mixrate','Microsed','490-2D','490 4+'];
function nhan(text){
 const t=String(text||'');if(!t)return[];const s=new Set();
 for(const [k,re] of LIST)if(re.test(t))s.add(k);
 /* Có chữ Furuno thì nhận cả "CA 400" viết cách */
 if(/furuno/i.test(t))for(const n of ['270','400','800'])if(new RegExp('(?<![a-z0-9])ca\\s+'+n+'(?!\\d)','i').test(t))s.add('CA-'+n);
 /* D-10/D-100 chỉ tính khi gói có ngữ cảnh HbA1c/Bio-Rad */
 if(!/hba1c|bio[\s-]*rad|hemoglobin|huyết sắc tố|đường huyết trung bình/i.test(t)){s.delete('D-10');s.delete('D-100')}
 if(![...s].some((x)=>x.startsWith('Celltac'))&&/cell?[\s-]*tac/i.test(t))s.add('Celltac');
 if(!s.has('Thrombolyzer XRC')&&!s.has('Thrombolyzer XRM')&&/thrombolyzer/i.test(t))s.add('Thrombolyzer');
 if(![...s].some((x)=>x.startsWith('Urilyzer'))&&/urilyzer/i.test(t))s.add('Urilyzer');
 for(const m of t.matchAll(/(?<![a-z0-9])mek[\s-]*(\d{3,4}[a-z]?)(?![a-z0-9])/gi))s.add('MEK-'+m[1].toUpperCase());
 /* Model Nihon Kohden → tên Celltac */
 for(const x of [...s]){const m=/^MEK-(\d{4})/.exec(x);if(m&&MEK[m[1]])s.add(MEK[m[1]])}
 for(const [b,re] of Object.entries(BRAND))if([...s].some((x)=>re.test(x)))s.add(b);
 const rank=(x)=>{let i=ORDER.indexOf(x);if(i<0&&x.startsWith('MEK-'))i=ORDER.indexOf('MEK-');return i<0?99:i};
 return [...s].sort((a,b)=>rank(a)-rank(b)||a.localeCompare(b));
}
const rank=(x)=>{let i=ORDER.indexOf(x);if(i<0&&x.startsWith('MEK-'))i=ORDER.indexOf('MEK-');return i<0?99:i};
const sort=(a)=>a.slice().sort((x,y)=>rank(x)-rank(y)||x.localeCompare(y));
g.NHAN={nhan,ORDER,sort};
})(typeof window!=='undefined'?window:globalThis);
if(typeof module!=='undefined')module.exports=globalThis.NHAN;
