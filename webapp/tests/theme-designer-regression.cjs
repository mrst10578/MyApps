'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
const html=fs.readFileSync('webapp/index.html','utf8');
const a=html.indexOf('let studioAdvancedThemeEnabled=false;');
const b=html.indexOf('function previewNewTheme(){',a);
assert.ok(a>=0&&b>a,'Advanced theme helpers available');
const tokens=['bg','dark','accent','soft','cell','line','muted'];
const palette={bg:'#f0f0f0',dark:'#111111',accent:'#3366aa',soft:'#dedede',cell:'#ffffff',line:'#cccccc',muted:'#555555'};
const edits=new Map(tokens.map(k=>[k,{value:palette[k]}]));
const elements={
 customThemeAccent:{value:'#3366aa'},customThemeBg:{value:'#f0f0f0'},
 customThemeInk:{value:'#111111'}
};
const context={document:{querySelector:s=>edits.get(s.match(/\[data-theme-token="(.*?)"\]/)?.[1])||null},
 $:key=>elements[key],derivedPalette:()=>({...palette}),
 contrast:(a,b)=>a===b?1:7,console};
vm.createContext(context);
vm.runInContext(html.slice(a,b),context);
const auto=context.studioThemeCurrentPalette();
assert.deepEqual(JSON.parse(JSON.stringify(auto)),palette);
vm.runInContext('studioAdvancedThemeEnabled=true',context);
edits.get('cell').value='#ffeeee';
assert.equal(context.studioThemeCurrentPalette().cell,'#ffeeee');
edits.get('cell').value='invalid';
assert.equal(context.studioThemeCurrentPalette().cell,palette.cell);
console.log('✓ automatic and manual palette modes validate tokens');
assert.equal(context.studioThemeContrastStatus(palette).ok,true);
context.contrast=()=>3;
assert.equal(context.studioThemeContrastStatus(palette).ok,false);
console.log('✓ readability test detects insufficient contrast');
assert.ok(html.includes('id="studioAdvancedThemeToggle"'));
assert.ok(html.includes('id="studioThemeContrastNote"'));
assert.ok(html.includes('studioThemeSetup();rebuild();'));
assert.ok(html.includes('if(studioAdvancedThemeEnabled&&!studioThemeContrastStatus(colors).ok)'));
for(const key of tokens)assert.ok(html.includes('data-theme-token="'+key+'"'));
console.log('✓ all seven editable tokens wired and unsafe save blocked');
const scripts=[...html.matchAll(/<script([^>]*)>([\s\S]*?)<\/script>/gi)].filter(m=>!m[1].includes('application/json'));
for(const script of scripts)new Function(script[2]);
console.log('✓ executable scripts parse');
