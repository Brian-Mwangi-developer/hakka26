import 'package:flutter_test/flutter_test.dart';
import 'package:hakka/main.dart';

void main() {
  testWidgets('shows the app name', (tester) async {
    await tester.pumpWidget(const HakkaApp());

    expect(find.text(appName), findsOneWidget);
  });
}
