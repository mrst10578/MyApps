'use strict';
const fs=require('fs'),vm=require('vm'),assert=require('node:assert/strict');
const html=fs.readFileSync('webapp/index.html','utf8');
const a=html.indexOf('function studioApplyCellRepeat(');
const b=html.indexOf('function studioOpenCellRepeat(',a);
assert.ok(a>=0&&b>a);
const days=['شنبه','یکشنبه','دوشنبه','سه‌شنبه','چهارشنبه','پنجشنبه','جمعه'];
const c={DAYS:days};vm.runInNewContext(html.slice(a,b),c);
const model={cells:{'شنبه|c1':'مطالعه زیست','یکشنبه|c1':'قدیمی','سه‌شنبه|c2':'مبحث دیگر'}};
c.studioApplyCellRepeat(model,'شنبه','c1',['یکشنبه','دوشنبه','شنبه','bad']);
assert.equal(model.cells['یکشنبه|c1'],'مطالعه زیست');
assert.equal(model.cells['دوشنبه|c1'],'مطالعه زیست');
assert.equal(model.cells['سه‌شنبه|c2'],'مبحث دیگر');
assert.equal(model.cells['bad|c1'],undefined);
assert.equal(model.cells['شنبه|c1'],'مطالعه زیست');
console.log('✓ repeat a single subject to selected valid days without changing others');
assert.ok(html.includes('studioChange(()=>studioApplyCellRepeat(state,day,col.id,targets))'));
assert.ok(html.includes("!confirm('بعضی خانه‌های مقصد متن دارند."));
assert.ok(html.includes('dialog.showModal()'));
assert.ok(html.includes("repeat.setAttribute('aria-label'"));
console.log('✓ repeat uses undo-aware mutation, explicit override approval, and accessible dialog');
for(const m of [...html.matchAll(/<script([^>]*)>([\s\S]*?)<\/script>/gi)]){
 if(!m[1].includes('application/json'))new Function(m[2]);
}
console.log('✓ scripts parse');
