import 'package:flutter_test/flutter_test.dart';
import 'package:hakka/main.dart';

void main() {
  testWidgets('shows the OTP title', (tester) async {
    await tester.pumpWidget(const HakkaApp());

    expect(find.text('Enter your code'), findsOneWidget);
  });
}
