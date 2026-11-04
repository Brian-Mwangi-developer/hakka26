import 'package:flutter/material.dart';

const appName = 'hakkahakka26';

void main() {
  runApp(const HakkaApp());
}

class HakkaApp extends StatelessWidget {
  const HakkaApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: appName,
      theme: ThemeData(brightness: Brightness.light),
      darkTheme: ThemeData(brightness: Brightness.dark),
      home: const HomeScreen(),
    );
  }
}

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Image.asset(
              'assets/thamani-logo.png',
              width: 64,
              height: 64,
              semanticLabel: 'Thamani logo',
            ),
            const SizedBox(height: 12),
            Text(appName, style: Theme.of(context).textTheme.headlineSmall),
          ],
        ),
      ),
    );
  }
}
