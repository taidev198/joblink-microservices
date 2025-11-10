// importWordsSql.js
const fs = require('fs');
const path = require('path');

// 1. Load the FE bundle
const bundlePath = '/Users/taint/Downloads/EnglishVocabularyApp/ios/App/App/public/assets/index-DhuO45O0.js';
const bundle = fs.readFileSync(bundlePath, 'utf8');

// 2. Extract the U array literal
// Extract the U = [...] array block until the closing bracket
const match = bundle.match(/const\s+U\s*=\s*(\[[\s\S]*?\]);/);
if (!match) {
  console.error('❌ Could not find U array in bundle');
  process.exit(1);
}
let raw = match[1];  // retain original JS literal

// 3. Parse the JS array via a temp module
let words;
try {
  // Write the exact literal to tmpU.js so Node parses it as JS
  const tempModule = path.join(__dirname, 'tmpU.js');
  fs.writeFileSync(tempModule, `module.exports = ${raw}`);
  words = require(tempModule);
  fs.unlinkSync(tempModule);
} catch (err) {
  console.error('❌ Parsing failed. Raw snippet:', raw.slice(0,200));
  console.error('Error:', err);
  process.exit(1);
}

// 4. Build SQL insert statements
const sqlPath = path.join(__dirname, 'insert_words.sql');
const values = words.map(w => {
  const eng = w.chunk.replace(/'/g, "''");
  const ex  = w.english.replace(/'/g, "''");
  const vn  = w.vietnamese.replace(/'/g, "''");
  // Minimal fields: english_word, example_sentence, translation, default level and category
  return `('${eng}', '', '${ex}', '${vn}', 'BEGINNER', 'DAILY_LIFE', true, NOW(), NOW())`;
});
const sql = `INSERT INTO words
  (english_word, meaning, example_sentence, translation, level, category, is_active, created_at, updated_at)
VALUES
${values.join(',\n')};`;

fs.writeFileSync(sqlPath, sql);
console.log(`✅ SQL file generated at ${sqlPath}`);
