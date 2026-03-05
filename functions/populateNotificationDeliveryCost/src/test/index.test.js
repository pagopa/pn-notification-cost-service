const { expect } = require('chai');
const proxyquire = require("proxyquire").noPreserveCache();

describe('index tests', function() {

    it("test Ok", async () => {
        const event = {}
        expect(true).to.be.true;
    });
});