import SwiftUI

struct ContentView: View {
    var body: some View {
        VStack(spacing: 12) {
            Image("ThamaniLogo")
                .resizable()
                .scaledToFit()
                .frame(width: 64, height: 64)
                .accessibilityLabel("Thamani logo")
            Text(AppInfo.name)
                .font(.title2)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

#Preview {
    ContentView()
}
