const path = require('path');
require('dotenv').config({ path: path.resolve(__dirname, './src/config/.env') });

const { runMigration } = require("./src/app/Migration.js");


const iunsToProcess = process.argv.slice(2);
runMigration(iunsToProcess);
