export function isValidIP(ip: string): boolean {
  const regex = /^((25[0-5]|2[0-4]\d|[01]?\d\d?)\.){3}(25[0-5]|2[0-4]\d|[01]?\d\d?)$/
  return regex.test(ip)
}

export function isValidMAC(mac: string): boolean {
  const regex = /^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$/
  return regex.test(mac)
}

export function isValidPhone(phone: string): boolean {
  const regex = /^1[3-9]\d{9}$/
  return regex.test(phone)
}

export function isValidEmail(email: string): boolean {
  const regex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/
  return regex.test(email)
}

export function isValidDeviceCode(code: string): boolean {
  return code.length >= 3 && code.length <= 50
}

export function isValidSerialNumber(serial: string): boolean {
  return serial.length >= 3 && serial.length <= 100
}
