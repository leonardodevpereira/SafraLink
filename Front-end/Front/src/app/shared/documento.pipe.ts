import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'documento', standalone: false })
export class DocumentoPipe implements PipeTransform {
  transform(valor: string | null | undefined): string {
    const d = (valor ?? '').replace(/\D/g, '');
    if (d.length === 11) {
      return `${d.slice(0, 3)}.${d.slice(3, 6)}.${d.slice(6, 9)}-${d.slice(9)}`;
    }
    if (d.length === 14) {
      return `${d.slice(0, 2)}.${d.slice(2, 5)}.${d.slice(5, 8)}/${d.slice(8, 12)}-${d.slice(12)}`;
    }
    return valor ?? '';
  }
}
