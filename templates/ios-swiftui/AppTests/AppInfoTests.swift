import XCTest
@testable import App

final class AppInfoTests: XCTestCase {
    func testAppNameIsHakkahakka26() {
        XCTAssertEqual(AppInfo.name, "hakkahakka26")
    }
}
