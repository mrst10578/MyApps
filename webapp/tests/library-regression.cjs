'use strict';
const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const html=fs.readFileSync('webapp/index.html','utf8');
const a=html.indexOf('function studioNormalizeLibrarySearch(value){');
const b=html.indexOf('function studioMemorize(){',a);
assert.ok(a>=0&&b>a,'Persian search helper exists');
const c={studioLibraryQuery:'',
 toEn:s=>String(s).replace(/[۰-۹]/g,d=>String('۰۱۲۳۴۵۶۷۸۹'.indexOf(d)))
   .replace(/[٠-٩]/g,d=>String('٠١٢٣٤٥٦٧٨٩'.indexOf(d)))};
vm.runInNewContext(html.slice(a,b),c);
const item={label:'داده های ۱۴۰۵',title:'روان‌شناسی',data:{title:'برنامه',startDate:'2026-10-10',columns:[{name:'فیزیک'},{name:'شيمي'}]}};
for(const [q,expected] of [['روانشناسي',true],['روان شناسی',true],['۱۴۰۵',true],['شیمی',true],['فيزيك',true],['ناشناخته',false]]){
 c.studioLibraryQuery=q;
 assert.equal(c.studioLibraryMatches(item),expected,'Query: '+q);
}
console.log('✓ tolerant Persian/Arabic search, digits, and study columns');
const ps=html.indexOf('function studioToggleArchivePin(id){');
const pe=html.indexOf('function archiveItemUI(item){',ps);
assert.ok(ps>=0&&pe>ps);
const pinStore=new Map();
const ctx={library:[{id:'one'}],studioPinned:new Set(),studioInitComplete:true,localStorage:{setItem:(k,v)=>pinStore.set(k,v)},
studioWriteIDB:async()=>true,renderLibrary:()=>{ctx.renders++},renders:0};
vm.runInNewContext(html.slice(ps,pe),ctx);
ctx.studioToggleArchivePin('one');
assert.equal(ctx.studioPinned.has('one'),true);
assert.deepEqual(JSON.parse(pinStore.get('barname-pinned')),['one']);
ctx.studioToggleArchivePin('one');
assert.equal(ctx.studioPinned.size,0);
ctx.studioToggleArchivePin('not-stored');
assert.equal(ctx.studioPinned.size,0);
assert.equal(ctx.renders,2);
console.log('✓ touch pin action persists and ignores missing archives');
assert.ok(html.includes("star.setAttribute('aria-pressed',String(pinned))"));
assert.ok(html.includes('saved-program-row'));
assert.ok(html.includes('.saved-pin:focus-visible'));
console.log('✓ separate touch-friendly accessible pin control');
