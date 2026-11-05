import { StatusBar } from 'expo-status-bar';
import { Image, StyleSheet, Text, useColorScheme, View } from 'react-native';

export const APP_NAME = 'hakkahakka26';

export default function App() {
  const isDarkMode = useColorScheme() === 'dark';

  return (
    <View style={[styles.container, isDarkMode && styles.containerDark]}>
      <Image
        source={require('./assets/thamani-logo.png')}
        style={styles.logo}
        resizeMode="contain"
        accessibilityLabel="Thamani logo"
      />
      <Text style={[styles.title, isDarkMode && styles.titleDark]}>{APP_NAME}</Text>
      <StatusBar style="auto" />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#FFFFFF',
  },
  containerDark: {
    backgroundColor: '#161A17',
  },
  logo: {
    width: 64,
    height: 64,
  },
  title: {
    fontSize: 24,
    color: '#111111',
  },
  titleDark: {
    color: '#F5F5F5',
  },
});
