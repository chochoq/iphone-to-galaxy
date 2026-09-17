// Runs one instrumentation process at a time, in the disposable TapTop006 emulator only.
import {execFileSync} from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
const [serial,output,...requested]=process.argv.slice(2);
if(!serial?.startsWith('emulator-')||!output)throw Error('Usage: node run-scroll-checks.mjs emulator-PORT fresh-output [modes...]');
if(!process.env.ANDROID_SDK_ROOT)throw Error('Set ANDROID_SDK_ROOT to the Android SDK directory');
const adb=process.env.ANDROID_SDK_ROOT+'/platform-tools/adb';
const run=args=>execFileSync(adb,['-s',serial,...args],{encoding:'utf8',timeout:120000});
if(!run(['emu','avd','name']).split(/\r?\n/).includes('TapTop006'))throw Error('Refusing to operate on a user AVD');
const out=path.resolve(output);if(fs.existsSync(out))throw Error('Use a fresh report directory');fs.mkdirSync(out,{recursive:true});
// ATD defaults to disabled drawing. Restart apps after changing this before visual tests.
if(run(['shell','getprop','debug.hwui.drawing_enabled']).trim()!=='1')throw Error('Enable ATD drawing and restart test/product apps before this run');
if(!run(['shell','dumpsys','accessibility']).includes('Service[label=맨 위로 톡'))throw Error('Product accessibility service must be bound');
const modes=requested.length?requested:['settings','service','regression','web','webcancel'];
for(const mode of modes){
 if(!['direct','settings','service','regression','web','webcancel','notifications','coverage','gridcontract','legacy','motion'].includes(mode))throw Error('Unknown mode '+mode);
 console.log('Running '+mode);
 const result=run(['shell','am','instrument','-w','-e','mode',mode,'com.chocho.scroll006test/.Checks']);
 fs.writeFileSync(path.join(out,mode+'.txt'),result);
 if(!/^PASS \d+ assertions$/m.test(result)||/^FAIL /m.test(result))throw Error('Failed '+mode+'; see '+out);
 console.log(result.match(/^PASS .+$/m)[0]);
}
console.log('Reports: '+out);
