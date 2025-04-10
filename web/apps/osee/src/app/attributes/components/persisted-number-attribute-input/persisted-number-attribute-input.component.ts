/*********************************************************************
 * Copyright (c) 2024 Boeing
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Boeing - initial API and implementation
 **********************************************************************/
import {
	ChangeDetectionStrategy,
	Component,
	computed,
	inject,
	input,
	model,
	signal,
} from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { ATTRIBUTETYPEID } from '@osee/attributes/constants';
import { attribute } from '@osee/attributes/types';
import { FocusLostInputComponent } from '@osee/shared/components';
import { applic } from '@osee/applicability/types';
import {
	provideOptionalControlContainerNgForm,
	writableSlice,
} from '@osee/shared/utils';
import { CurrentTransactionService } from '@osee/transactions/services';
import {
	pairwise,
	map,
	combineLatest,
	switchMap,
	take,
	of,
	filter,
	takeUntil,
} from 'rxjs';
import { ControlContainer } from '@angular/forms';

@Component({
	selector: 'osee-persisted-number-attribute-input',
	imports: [FocusLostInputComponent],
	changeDetection: ChangeDetectionStrategy.OnPush,
	template: ` <osee-focus-lost-input
		[disabled]="disabled()"
		type="number"
		[(value)]="attrValue"
		[label]="label()"
		[tooltip]="tooltip()">
		<ng-content></ng-content>
	</osee-focus-lost-input>`,
	viewProviders: [provideOptionalControlContainerNgForm()],
})
export class PersistedNumberAttributeInputComponent<U extends ATTRIBUTETYPEID> {
	private control = signal(inject(ControlContainer));
	private statusSignal = computed(() => {
		const _control = this.control();
		const _statusChanges = _control.statusChanges;
		if (_control && _statusChanges) {
			return _statusChanges;
		}
		return of('INVALID');
	});
	artifactId = input.required<`${number}`>();
	artifactApplicability = input.required<applic>();
	label = input('');
	tooltip = input('');

	comment = input('Modifying attribute');
	value = model.required<attribute<number | `${number}`, U>>();
	protected attrValue = writableSlice(this.value, 'value');
	disabled = input(false);

	private _notValid = this.statusSignal().pipe(
		filter((v) => v === 'INVALID' || v === 'DISABLED')
	);
	private _value$ = toObservable(this.value).pipe(takeUntil(this._notValid));

	private _valueUpdated = this._value$.pipe(
		takeUntil(this._notValid),
		pairwise(),
		map(
			([prev, curr]) =>
				prev.value !== curr.value &&
				prev.id === curr.id &&
				prev.gammaId === curr.gammaId
		)
	);

	private _artifactId$ = toObservable(this.artifactId);
	private _artifactApplicability$ = toObservable(this.artifactApplicability);

	private _comment$ = toObservable(this.comment);

	private _currentTxService = inject(CurrentTransactionService);

	private _tx = toSignal(
		combineLatest([this._valueUpdated, this.statusSignal()]).pipe(
			filter(([updated, validity]) => updated && validity === 'VALID'),
			switchMap((_) =>
				combineLatest([
					this._value$,
					this._artifactId$,
					this._artifactApplicability$,
					this._comment$,
				]).pipe(
					take(1),
					switchMap(([value, artId, applic, comment]) => {
						const attrs =
							value.id === '-1' && value.gammaId === '-1'
								? { add: [value] }
								: { set: [value] };
						return this._currentTxService
							.modifyArtifactAndMutate(
								comment,
								artId,
								applic,
								attrs
							)
							.pipe(take(1));
					})
				)
			)
		)
	);
}
