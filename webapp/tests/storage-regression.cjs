'use strict';
const fs=require('node:fs'),assert=require('node:assert/strict');
const source=fs.readFileSync('webapp/index.html','utf8');
const a='const libraryReady=(async()=>{',b='let studioHadLocalDraft=';
assert.ok(source.includes(a)&&source.includes(b));
assert.ok(source.includes("const ARCHIVE_REV_KEY='barname-archive-authoritative-v3'"));
assert.ok(source.includes('studioEditedDuringRestore'));
assert.ok(source.includes('draft:state}'));
const code=a+source.split(a)[1].split(b)[0];
const entry=id=>({id,updatedAt:1,data:{columns:[{id:'c1'}],cells:{}}});
const pause=ms=>new Promise(r=>setTimeout(r,ms));
async function make(list,rev,legacy,ms=0){
 const map=new Map();
 if(list!==null)map.set('library',JSON.stringify(list));
 if(rev!==null)map.set('rev',rev);
 const store={getItem:k=>map.get(k)||null,setItem:(k,v)=>map.set(k,String(v))};
 const older={data:legacy},mirror={data:[]};
 const implementation=[
   "const LIB_KEY='library',ARCHIVE_REV_KEY='rev';",
   "let library=JSON.parse(localStorage.getItem(LIB_KEY)||'[]'),archiveMutation=0,archivePersisted=null,studioInitComplete=true;",
   "const $=()=>null,renderLibrary=()=>{},updateArchiveStorageStatus=()=>{};",
   "const openArchiveDB=Promise.resolve({});",
   "const getArchiveFromDB=async()=>older.data;",
   "const putArchiveInDB=async(db,items)=>{await pause(ms);older.data=JSON.parse(JSON.stringify(items));return true};",
   "const studioWriteIDB=async(key,items)=>{mirror.data=JSON.parse(JSON.stringify(items));return true};",
   code,
   "return {ready:libraryReady,persistArchive,get:()=>library,local:()=>JSON.parse(localStorage.getItem(LIB_KEY)||'[]'),older:()=>older.data,mirror:()=>mirror.data,revision:()=>localStorage.getItem('rev')};"
 ].join('\n');
 const env=new Function('localStorage','older','mirror','pause','ms',implementation)(store,older,mirror,pause,ms);
 await env.ready;return env;
}
(async()=>{
 const migration=await make([entry('local')],null,[entry('older')]);
 assert.deepEqual(migration.get().map(x=>x.id),['local','older']);
 await migration.persistArchive(migration.get());
 const deleted=await make([entry('gone'),entry('keep')],'present',[entry('gone'),entry('keep')]);
 await deleted.persistArchive([entry('keep')]);
 const again=await make(deleted.local(),deleted.revision(),[entry('gone'),entry('keep')]);
 assert.deepEqual(again.get().map(x=>x.id),['keep']);
 const parallel=await make([],'present',[],15);
 assert.deepEqual(await Promise.all([parallel.persistArchive([entry('one')]),parallel.persistArchive([entry('two')]),parallel.persistArchive([])]),[true,true,true]);
 assert.deepEqual(parallel.local(),[]);
 assert.deepEqual(parallel.older(),[]);
 assert.deepEqual(parallel.mirror(),[]);
 console.log('PASS: migration, deletion tombstone, serialized saves, portable backup guards');
})().catch(e=>{console.error(e);process.exitCode=1});
