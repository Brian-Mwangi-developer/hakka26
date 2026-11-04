import 'dart:async';

import 'package:flutter/material.dart';

void main() {
  runApp(const HakkaApp());
}

class HakkaApp extends StatelessWidget {
  const HakkaApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      theme: ThemeData(brightness: Brightness.light),
      darkTheme: ThemeData(brightness: Brightness.dark),
      home: const OtpPage(),
    );
  }
}

class OtpPage extends StatefulWidget {
  const OtpPage({super.key});

  @override
  State<OtpPage> createState() => _OtpPageState();
}

class _OtpPageState extends State<OtpPage> {
  static const _green = Color(0xFF12361B);
  final _controllers = List.generate(6, (_) => TextEditingController());
  final _focusNodes = List.generate(6, (_) => FocusNode());
  String _message = '';
  int _secondsLeft = 30;
  Timer? _timer;

  @override
  void initState() {
    super.initState();
    _startTimer();
  }

  void _startTimer() {
    _timer?.cancel();
    setState(() => _secondsLeft = 30);
    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (_secondsLeft == 0) {
        timer.cancel();
      } else {
        setState(() => _secondsLeft--);
      }
    });
  }

  bool get _complete => _controllers.every((c) => c.text.isNotEmpty);

  void _submit() {
    final code = _controllers.map((c) => c.text).join();
    setState(() {
      if (code == '123456') {
        _message = "You're in.";
      } else {
        _message = "That code didn't work. Try again.";
        for (final controller in _controllers) {
          controller.clear();
        }
        _focusNodes.first.requestFocus();
      }
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    for (final controller in _controllers) {
      controller.dispose();
    }
    for (final node in _focusNodes) {
      node.dispose();
    }
    super.dispose();
  }

  Widget _box(int index) {
    final filled = _controllers[index].text.isNotEmpty;
    final focused = _focusNodes[index].hasFocus;
    return Container(
      width: 46,
      height: 46,
      margin: const EdgeInsets.symmetric(horizontal: 5),
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        color: filled ? _green : (focused ? Colors.white : const Color(0xFFF1F1F1)),
        border: Border.all(color: focused ? Colors.black : const Color(0xFFE0E0E0)),
      ),
      alignment: Alignment.center,
      child: TextField(
        controller: _controllers[index],
        focusNode: _focusNodes[index],
        autofocus: index == 0,
        keyboardType: TextInputType.number,
        textAlign: TextAlign.center,
        maxLength: 1,
        inputFormatters: [FilteringTextInputFormatter.digitsOnly],
        style: const TextStyle(color: Colors.white, fontSize: 20),
        decoration: const InputDecoration(counterText: '', border: InputBorder.none, hintText: '0'),
        onChanged: (value) {
          setState(() {});
          if (value.isNotEmpty && index < 5) {
            _focusNodes[index + 1].requestFocus();
          }
        },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black54,
      body: Align(
        alignment: Alignment.bottomCenter,
        child: Container(
          height: MediaQuery.of(context).size.height * 0.85,
          padding: const EdgeInsets.all(24),
          decoration: const BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.vertical(top: Radius.circular(28)),
          ),
          child: Column(
            children: [
              Image.asset('assets/thamani-logo.png', width: 56, height: 56),
              const SizedBox(height: 20),
              const Text('Enter your code', style: TextStyle(fontSize: 26, fontWeight: FontWeight.w500)),
              const SizedBox(height: 8),
              const Text('A 6-digit code has been sent to your phone.'),
              const SizedBox(height: 28),
              Row(mainAxisAlignment: MainAxisAlignment.center, children: List.generate(6, _box)),
              const SizedBox(height: 24),
              TextButton(
                onPressed: _secondsLeft == 0 ? _startTimer : null,
                child: Text(_secondsLeft == 0 ? "Didn't receive the code? Resend" : 'Resend in 0:$_secondsLeft'),
              ),
              Text(_message),
              const Spacer(),
              SizedBox(
                width: double.infinity,
                height: 56,
                child: ElevatedButton(
                  onPressed: _complete ? _submit : null,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: _green,
                    foregroundColor: Colors.white,
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(28)),
                  ),
                  child: const Text('Submit'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
