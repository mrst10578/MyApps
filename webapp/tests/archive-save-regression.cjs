'use strict';
const fs=require('fs'),vm=require('vm'),assert=require('assert/strict');
const source=fs.readFileSync('webapp/index.html','utf8');
const a=source.indexOf('async function saveArchiveAfterExport('),b=source.indexOf('function scheduleSave()',a);
assert.ok(a>=0&&b>a,'Archive export handler exists');
const code=source.slice(a,b);
async function makeEnvironment(success){
 const notices=[],old={id:'old',data:{title:'قدیمی'},kind:'self'};
 const context={
  library:[old],libraryReady:Promise.resolve(),archiveMutation:0,
  currentPlanId:'some-other-plan',selectedLibraryId:'old',
  state:{audience:'self'},save:()=>{},uid:()=> 'new',
  normalize:x=>x,timestampInfo:()=>({display:'test'}),
  persistArchive:async()=>success,
  toast:x=>notices.push(x),
  document:{querySelectorAll:()=>[]},console
 };
 vm.runInNewContext(code,context);
 return {context,notices,old};
}
(async()=>{
 const bad=await makeEnvironment(false);
 const rejected=await bad.context.saveArchiveAfterExport({title:'جدید'},'new','self');
 assert.equal(rejected,false);
 assert.equal(bad.context.library.length,1);
 assert.equal(bad.context.library[0].id,'old');
 assert.equal(bad.context.selectedLibraryId,'old');
 console.log('✓ failed persistence never replaces the visible archive');
 const good=await makeEnvironment(true);
 const accepted=await good.context.saveArchiveAfterExport({title:'جدید'},'new','self');
 assert.equal(accepted,true);
 assert.equal(good.context.library.length,2);
 assert.equal(good.context.library[0].id,'new');
 assert.equal(good.context.selectedLibraryId,'new');
 console.log('✓ successful persistence commits archive in memory');
 const full=await makeEnvironment(true);
 full.context.library=Array.from({length:150},(_,i)=>({id:'saved'+i,data:{}}));
 const rejectedFull=await full.context.saveArchiveAfterExport({title:'پر'},'new','self');
 assert.equal(rejectedFull,false);
 assert.equal(full.context.library.length,150);
 console.log('✓ archive capacity enforced without deleting user content');
})().catch(e=>{console.error(e);process.exitCode=1});
