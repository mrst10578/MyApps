'use strict';
const fs=require('node:fs');
const vm=require('node:vm');
const assert=require('node:assert/strict');
const html=fs.readFileSync('webapp/index.html','utf8');
const start='function barnamePrepareBackup(input){',end='async function importPrograms(input){';
const a=html.indexOf(start),b=html.indexOf(end,a);
assert.ok(a>=0&&b>a,'Backup preflight function must be available');
const source=html.slice(a,b);
const palette={bg:'#eeeeee',dark:'#222222',accent:'#3388ee',soft:'#dddddd',cell:'#ffffff',line:'#cccccc',muted:'#777777'};
let sequence=0;
const env={
 PALETTES:{ocean:palette},
 customThemes:[],
 studioSavedTemplates:[],
 library:[],
 uid:()=> 'plan-'+(++sequence),
 console
};
env.normalize=x=>{
 if(!x||!Array.isArray(x.columns)||x.columns.length===0)throw Error('Bad plan');
 return {title:String(x.title||'برنامه').slice(0,90),
  theme:env.PALETTES[x.theme]?x.theme:'ocean',
  audience:x.audience==='others'?'others':'self',
  startDate:x.startDate||'2026-10-10',
  columns:x.columns.map(c=>({id:c.id,name:c.name})),
  cells:x.cells||{}};
};
vm.runInNewContext(source,env);
const prepare=env.barnamePrepareBackup;
const data=(title='برنامه',theme='ocean')=>({title,theme,columns:[{id:'c1',name:'زیست'}],cells:{'شنبه|c1':'درس'}});
const pack=(plans,more={})=>({format:'hafteh-negar-library',version:5,plans,...more});
const plain=x=>JSON.parse(JSON.stringify(x));
assert.throws(()=>prepare({format:'other',plans:[]}),/معتبر/);
assert.throws(()=>prepare(pack(Array.from({length:201},(_,i)=>({data:data(String(i))})))),/بیش از حد/);
console.log('✓ rejects malformed and excessive backups');
const original=pack([{data:data('الف')},{data:data('الف')},{data:{columns:[]}}]);
const snapshot=JSON.stringify(original);
let preview=prepare(original);
assert.equal(preview.plans.length,1);
assert.equal(preview.skippedDuplicate,1);
assert.equal(preview.skippedInvalid,1);
assert.equal(JSON.stringify(original),snapshot);
assert.equal(env.library.length,0);
console.log('✓ preflight is read-only, detects duplicates and invalid plans');
env.library=[{id:'existing',kind:'self',data:data('الف')}];
preview=prepare(pack([{data:data('الف')},{data:data('ب')}]));
assert.equal(preview.plans.length,1);
assert.equal(preview.plans[0].title,'ب');
console.log('✓ skips an existing saved program');
const newPalette={...palette,accent:'#fa8a11'};
preview=prepare(pack([{data:data('با تم شخصی','user-amber')},{data:data('با تم شخصی','user-amber')}],{
 themes:[{id:'user-amber',label:'کهربایی',colors:newPalette}],
 templates:[{label:'قالب۱',data:data('قالب آزمایشی','user-amber')}],
 draft:data('پیش‌نویس','user-amber')
}));
assert.equal(preview.themes.length,1);
assert.equal(preview.plans.length,2);
assert.equal(preview.templates.length,1);
assert.equal(preview.plans[0].data.theme,preview.themes[0].id);
assert.equal(preview.plans[1].title,'پیش‌نویس');
assert.equal(preview.skippedDuplicate,1);
console.log('✓ restores themes, templates and draft without mutating current editor');
env.PALETTES['user-amber']={...palette,accent:'#123456'};
env.customThemes=[{id:'user-amber',label:'existing',colors:env.PALETTES['user-amber']}];
preview=prepare(pack([{data:data('برنامه متفاوت','user-amber')}],{
 themes:[{id:'user-amber',label:'new',colors:newPalette}]
}));
assert.equal(preview.themes.length,1);
assert.notEqual(preview.themes[0].id,'user-amber');
assert.equal(preview.plans[0].data.theme,preview.themes[0].id);
console.log('✓ remaps colliding custom theme identifiers');
env.library=Array.from({length:150},(_,i)=>({id:'old'+i,kind:'self',data:data('old'+i)}));
preview=prepare(pack([{data:data('new') }]));
assert.equal(preview.plans.length,0);
assert.equal(preview.skippedCapacity,1);
console.log('✓ respects archive capacity, without deleting old programs');
